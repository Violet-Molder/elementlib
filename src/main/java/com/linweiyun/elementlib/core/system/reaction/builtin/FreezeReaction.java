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
 */
public class FreezeReaction extends ElementalReaction {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    private static final float FROZEN_MULTIPLIER = 2.0f;

    public FreezeReaction(Supplier<ElementalReactionType> type,
                          String elementAId, String elementBId,
                          float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }
    @Override
    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        if (ModElements.is(attackerElement, ModElements.FROZEN)) return false;
        if (ModElements.is(defenderElement, ModElements.FROZEN)) return false;
        return super.canMatch(attackerElement, defenderElement);
    }
    @Override
    public boolean canConsume(ElementalAttachmentInstance instance, GenshinElement slotElement) {
        if (ModElements.is(instance.getElement(), ModElements.FROZEN)) return false;
        return super.canConsume(instance, slotElement);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        return ModElements.is(context.attackerElement(), ModElements.FROZEN);
    }
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

        ElementalHost frozenHost = ctx.targetHost();
        if (frozenHost == null && ctx.targetEntity() != null) {
            frozenHost = EntityHost.of(ctx.targetEntity());
        }
        if (frozenHost != null) {
            ElementalAttachmentHelper.attachInternal(
                    frozenHost, frozen, AttachmentSource.SPECIAL, frozenProfile);
        } else {
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

    @Override
    public boolean showsIndicator(ReactionContext context) {
        return !(context.targetHost() instanceof BlockHost);
    }
}
