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

        float[] consumed = attackerIsA
                ? calculateConsumption(attackerQty, totalDefenderQty)
                : calculateConsumption(totalDefenderQty, attackerQty);
        float consumedA = consumed[0];
        float consumedB = consumed[1];

        consumeElementUnit(ctx.targetContainer(), elB, consumedB);
        consumeElementUnit(ctx.targetContainer(), elA, consumedA);

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
        Integer waterLevel = BlockElementStore.takeWaterLevel(blockHost.level(), blockHost.blockPos());
        BlockElementStore.clear(blockHost.level(), blockHost.blockPos());
        BlockState water = Blocks.WATER.defaultBlockState();
        if (waterLevel != null) {
            water = water.setValue(BlockStateProperties.LEVEL, waterLevel);
        }
        blockHost.level().setBlock(blockHost.blockPos(), water, 2);
    }

    @Override
    public boolean showsIndicator(ReactionContext context) {
        return !(context.targetHost() instanceof BlockHost);
    }
}
