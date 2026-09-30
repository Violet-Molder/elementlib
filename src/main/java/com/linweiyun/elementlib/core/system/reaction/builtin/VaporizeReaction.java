package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.reaction.ReactionConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 蒸发反应 —— 增幅反应，火:水 = 1:2。
 */
public class VaporizeReaction extends ElementalReaction {

    private static float getDominantMultiplier() { return ReactionConfig.VAPORIZE.get().floatValue(); }
    private static float getSubmissiveMultiplier() { return ReactionConfig.VAPORIZE_NEGATIVE.get().floatValue(); }

    public VaporizeReaction(Supplier<ElementalReactionType> type,
                            String elementAId, String elementBId,
                            float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        return ReactionPriorityCalculator.hasFrozen(context.targetContainer());
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();
        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);

        ElementalAttachmentInstance defInstance = findDefenderInstance(ctx, attackerIsA ? elB : elA);

        if (defInstance == null || defInstance.isFinished()) {
            return ReactionResult.builder(type()).build();
        }

        float attackerQty = ctx.attackerUnit();
        float defenderQty = defInstance.getUnit();

        float[] consumed = attackerIsA
                ? calculateConsumption(attackerQty, defenderQty)
                : calculateConsumption(defenderQty, attackerQty);
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

    @Nullable
    private ElementalAttachmentInstance findDefenderInstance(ReactionContext ctx,
                                                             @Nullable GenshinElement targetMain) {
        if (targetMain == null) return null;
        ElementalAttachmentInstance subElementMatch = null;
        for (StatusInstance inst : ctx.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            GenshinElement eaElement = ea.getElement();
            if (eaElement.getMainElement() != targetMain) continue;
            if (eaElement == targetMain) return ea;
            if (subElementMatch == null) subElementMatch = ea;
        }
        return subElementMatch;
    }
}
