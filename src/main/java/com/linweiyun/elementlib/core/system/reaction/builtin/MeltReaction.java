package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.reaction.ReactionConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.block.BlockElementRules;
import com.linweiyun.elementlib.core.system.about.block.BlockElementStore;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * 融化反应 —— 增幅反应，火:冰 = 1:2。
 *
 * <p>先手冰（含冻）后手火（火克冰）用 {@code MELT} 倍率，反之为 {@code MELT_NEGATIVE}。
 * 冰与冻通过 {@code getMainElement} 归并到冰，可一起被消耗。
 */
public class MeltReaction extends ElementalReaction {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    private static float getDominantMultiplier() { return ReactionConfig.MELT.get().floatValue(); }
    private static float getSubmissiveMultiplier() { return ReactionConfig.MELT_NEGATIVE.get().floatValue(); }

    public MeltReaction(Supplier<ElementalReactionType> type,
                        String elementAId, String elementBId,
                        float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();
        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);

        GenshinElement defenderTarget = attackerIsA ? elB : elA;
        float totalDefenderQty = sumMainElementQuantity(ctx, defenderTarget);

        if (totalDefenderQty <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        float attackerQty = ctx.attackerUnit();

        // 后手是 A 时 (attackerQty, totalDefenderQty) 就是 (A, B)；否则要换位。
        float[] consumed = attackerIsA
                ? calculateConsumption(attackerQty, totalDefenderQty)
                : calculateConsumption(totalDefenderQty, attackerQty);
        float consumedA = consumed[0];
        float consumedB = consumed[1];

        consumeElementUnit(ctx.targetContainer(), elB, consumedB);
        consumeElementUnit(ctx.targetContainer(), elA, consumedA);

        // 后手是克制方（attackerIsA）时吃高倍率。
        float configured = attackerIsA ? getDominantMultiplier() : getSubmissiveMultiplier();
        float multiplier = ReactionDamage.amplify(type(), configured);
        float consumedAttacker = attackerIsA ? consumedA : consumedB;

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(consumedAttacker)
                .consumedDefender(attackerIsA ? consumedB : consumedA)
                .amplified(multiplier)
                .build();
    }

    private float sumMainElementQuantity(ReactionContext ctx, GenshinElement mainTarget) {
        float sum = 0f;
        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().getMainElement() == mainTarget) {
                sum += ea.getUnit();
            }
        }
        return sum;
    }

    /**
     * 融化对<b>方块</b>的意义：冰被火吃掉，这一格变成水。
     *
     * <p>只在宿主是方块且这一格属于冰族时生效。这里连容器里的冰 / 冻一起清掉：调用方之后还会
     * 按容器内容跑一次状态迁移，留着冻元素会把刚化开的水又冻回去。
     */
    @Override
    public void applyHostEffect(ReactionContext context) {
        if (!(context.targetHost() instanceof BlockHost blockHost) || !blockHost.isValid()) {
            return;
        }
        BlockState state = blockHost.state();
        if (!BlockElementRules.isIceFamily(state)) {
            return;
        }
        for (StatusInstance inst : new ArrayList<>(context.targetContainer().getAll())) {
            if (inst instanceof ElementalAttachmentInstance ea
                    && (ModElements.is(ea.getElement(), ModElements.CYRO)
                    || ModElements.is(ea.getElement(), ModElements.FROZEN))) {
                context.targetContainer().remove(inst);
            }
        }
        // 化回来要还原成冻之前的水位；没有记录（天然冰）则按完整水源处理。
        Integer waterLevel = BlockElementStore.takeWaterLevel(blockHost.level(), blockHost.blockPos());
        BlockElementStore.clear(blockHost.level(), blockHost.blockPos());
        BlockState water = Blocks.WATER.defaultBlockState();
        if (waterLevel != null) {
            water = water.setValue(BlockStateProperties.LEVEL, waterLevel);
        }
        // flag=2：只把新状态发给客户端，不触发邻居更新，避免水流立刻重算覆盖水位。
        blockHost.level().setBlock(blockHost.blockPos(), water, 2);
    }

    /** 方块上的形态变化（水结冰 / 冰化水）不显示文字；生物身上照常。 */
    @Override
    public boolean showsIndicator(ReactionContext context) {
        return !(context.targetHost() instanceof BlockHost);
    }
}
