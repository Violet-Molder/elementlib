package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 冻结反应 —— 特殊反应，水:冰 = 1:1。
 *
 * <p>双方同时消耗，生成的冻元素量 = 消耗总量 × {@value #FROZEN_MULTIPLIER}。
 * 冻本身不参与冻结（已经冻住不能再冻），也不参与这一侧的消耗。
 *
 * <p>注册参数：{@code elementA = 水}、{@code elementB = 冰}，无克制方，不影响伤害。
 */
public class FreezeReaction extends ElementalReaction {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    private static final float FROZEN_MULTIPLIER = 2.0f;

    public FreezeReaction(Supplier<ElementalReactionType> type,
                          String elementAId, String elementBId,
                          float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    /** 冻元素不参与冻结反应的任何一侧配对。 */
    @Override
    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        if (ModElements.is(attackerElement, ModElements.FROZEN)) return false;
        if (ModElements.is(defenderElement, ModElements.FROZEN)) return false;
        return super.canMatch(attackerElement, defenderElement);
    }

    /** 覆盖基类的主元素归并：排除冻实例，只让精确的水 / 冰参与消耗。 */
    @Override
    public boolean canConsume(ElementalAttachmentInstance instance, GenshinElement slotElement) {
        if (ModElements.is(instance.getElement(), ModElements.FROZEN)) return false;
        return super.canConsume(instance, slotElement);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        return ModElements.is(context.attackerElement(), ModElements.FROZEN);
    }

    /**
     * 冻结的消耗与生成：算出双方消耗量后扣减，再按消耗总量 × {@value #FROZEN_MULTIPLIER} 生成冻元素。
     *
     * <p>{@code attacker} 指后手（本次附着的一方），{@code defender} 指先手（目标身上已有的）。
     * 后手可能是注册时的 A 也可能是 B，用 {@code attackerIsA} 映射回固定槽位。
     * 方块端没有实体：直接向容器添加冻，不走实体附着。
     */
    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();

        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);
        GenshinElement defenderTarget = attackerIsA ? elB : elA;

        float totalDefenderUnit = sumConsumable(ctx.targetContainer(), defenderTarget);

        if (totalDefenderUnit <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        float attackerQty = ctx.attackerUnit();

        float[] consumed = attackerIsA
                ? calculateConsumption(attackerQty, totalDefenderUnit)
                : calculateConsumption(totalDefenderUnit, attackerQty);
        float consumedA = consumed[0];
        float consumedB = consumed[1];

        float totalConsumed = consumedA + consumedB;
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        // 消耗前先记下这一格 / 这个目标上原有的水冰来源，挂到新生成的冻上。
        Set<String> cyroKeysBefore = new LinkedHashSet<>();
        Set<String> hydroKeysBefore = new LinkedHashSet<>();
        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            String key = ea.getSourceCharacterKey();
            if (key == null || key.isEmpty()) continue;
            if (ModElements.is(ea.getElement(), ModElements.CYRO)) {
                cyroKeysBefore.add(key);
            } else if (ModElements.is(ea.getElement(), ModElements.HYDRO)) {
                hydroKeysBefore.add(key);
            } else if (ModElements.is(ea.getElement(), ModElements.FROZEN)) {
                cyroKeysBefore.addAll(ea.getFrozenCyroSourceKeys());
                hydroKeysBefore.addAll(ea.getFrozenHydroSourceKeys());
            }
        }

        consumeElementUnit(ctx.targetContainer(), elB, consumedB);
        consumeElementUnit(ctx.targetContainer(), elA, consumedA);

        generateFrozen(ctx, totalConsumed, cyroKeysBefore, hydroKeysBefore);

        GenshinElement defenderElementConsumed = attackerIsA ? elB : elA;
        float defenderConsumedQty = attackerIsA ? consumedB : consumedA;

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(defenderConsumedQty)
                .build();
    }

    /** 生成冻元素附着，并把水冰来源键与冻结衰减状态带过去。 */
    private void generateFrozen(ReactionContext ctx, float totalConsumed,
                                Set<String> cyroKeysBefore, Set<String> hydroKeysBefore) {
        if (totalConsumed <= 0f) {
            return;
        }
        GenshinElement frozen = ModElements.of(ModElements.FROZEN);
        if (frozen == null) {
            return;
        }

        float frozenQty = totalConsumed * FROZEN_MULTIPLIER;
        AttachmentProfile frozenProfile = new AttachmentProfile(frozenQty, 1.0f, 0.0f, 999.0f);

        // 冻也是附着，同样走宿主入口（宿主筛查、覆盖规则照走）；用 attachInternal 避免递归触发反应。
        ElementalHost frozenHost = ctx.targetHost();
        if (frozenHost == null && ctx.targetEntity() != null) {
            frozenHost = EntityHost.of(ctx.targetEntity());
        }
        if (frozenHost != null) {
            ElementalAttachmentHelper.attachInternal(
                    frozenHost, frozen, AttachmentSource.SPECIAL, frozenProfile);
        } else {
            // 没有宿主信息时退回直接写容器，至少不让冻结丢效果。
            ctx.targetContainer().add(new ElementalAttachmentInstance(
                    frozen, AttachmentSource.SPECIAL, frozenProfile, frozenQty));
        }

        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (!ModElements.is(ea.getElement(), ModElements.FROZEN)) continue;
            for (String k : cyroKeysBefore) ea.addFrozenCyroSource(k);
            for (String k : hydroKeysBefore) ea.addFrozenHydroSource(k);
        }

        StatusContainer container = ctx.targetContainer();
        if (container.getFrozenDecayState() != null) {
            container.getFrozenDecayState().activate();
        }
    }

    /** 方块上的形态变化（水结冰 / 冰化水）不显示文字；生物身上照常。 */
    @Override
    public boolean showsIndicator(ReactionContext context) {
        return !(context.targetHost() instanceof BlockHost);
    }
}
