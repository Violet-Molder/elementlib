package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.api.ReactionCategory;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 元素反应管理器 —— 全静态方法。
 */
public class ElementalReactionManager {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    public static boolean canElementReact(GenshinElement attackerElement,
                                          StatusContainer container) {
        return canElementReact(attackerElement, container, null);
    }

    public static boolean canElementReact(GenshinElement attackerElement,
                                          StatusContainer container,
                                          @Nullable ElementalHost host) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        if (attackerMain == null) return false;

        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().isInstant()) continue;
            if (ElementRoles.is(ea.getElement(), ElementRoles.FYSIKOS)) continue;

            GenshinElement defenderMain = ea.getElement().getMainElement();
            if (defenderMain == null) continue;

            for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
                if (!reaction.canMatch(attackerMain, defenderMain)) continue;
                if (!acceptsReaction(host, attackerElement, ea.getElement(), reaction)) continue;
                return true;
            }
        }
        return false;
    }

    public static ReactionResult tryReactFor(@Nullable ElementalHost host, ReactionContext context) {
        return runReactions(context, reactionType -> {
            if (host == null) {
                return;
            }
            if (host.level() != null && host.blockPos() != null) {
                ReactionFeedback.atBlock(host.level(), host.blockPos(), reactionType);
                return;
            }
            var anchor = host.indicatorAnchor();
            if (anchor != null) {
                ReactionFeedback.reaction(anchor, reactionType, null);
            }
        });
    }
    @FunctionalInterface
    private interface ReactionIndicator {
        void show(ElementalReactionType reactionType);
    }

    private static ReactionResult runReactions(ReactionContext context, ReactionIndicator indicator) {
        float remainingAttackerQty = context.attackerUnit();
        List<ElementalAttachmentInstance> defenders = collectDefenders(context);
        if (defenders.isEmpty()) {
            return ReactionResult.builder(null).build();
        }

        List<Candidate> candidates = buildCandidates(context, defenders);

        ReactionResult firstAmplified = null;
        boolean anyReactionOccurred = false;
        List<ElementalReactionType> reactionTypes = new ArrayList<>();

        for (Candidate cand : candidates) {
            if (remainingAttackerQty <= 0f) break;
            if (cand.defender.isFinished()) continue;

            ReactionContext roundContext = new ReactionContext(
                    context.attackerElement(),
                    remainingAttackerQty,
                    context.attackerSource(),
                    context.attackerProfile(),
                    context.sourceKey(),
                    context.attackerEntity(),
                    context.targetContainer(),
                    context.targetEntity(),
                    context.targetHost()
            );

            ReactionResult result = cand.reaction.execute(roundContext);

            if (result == null || !result.isReacted()) continue;
            try {
                cand.reaction.applyHostEffect(roundContext);
            } catch (Throwable t) {
                LOGGER.error(" [Reaction] 宿主效果执行失败 reaction={}", cand.reaction.getReactionType(), t);
            }
            anyReactionOccurred = true;
            remainingAttackerQty -= result.getConsumedAttacker();
            if (cand.reaction.showsIndicator(roundContext)) {
                ElementalReactionType type = result.getReactionType();
                if (type != null) {
                    reactionTypes.add(type);
                }
            }
            if (firstAmplified == null && result.isAmplified()) {
                firstAmplified = result;
            }
        }

        if (anyReactionOccurred) {
            applyAttackerResidual(context, remainingAttackerQty);
            for (ElementalReactionType rt : reactionTypes) {
                if (shouldShowIndicator(rt)) indicator.show(rt);
            }
        }

        return firstAmplified != null ? firstAmplified :
                ReactionResult.builder(null).build();
    }

    private static boolean shouldShowIndicator(@Nullable ElementalReactionType reactionType) {
        if (reactionType == null) return false;
        ReactionCategory category = reactionType.getCategory();
        return category != ReactionCategory.LUNAR && category != ReactionCategory.STELLAR;
    }

    private static List<ElementalAttachmentInstance> collectDefenders(ReactionContext context) {
        List<ElementalAttachmentInstance> result = new ArrayList<>();
        GenshinElement attackerMain = context.attackerElement().getMainElement();

        for (StatusInstance inst : context.targetContainer().getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            GenshinElement defMain = ea.getElement().getMainElement();
            if (defMain == attackerMain) continue;
            if (ElementRoles.is(ea.getElement(), ElementRoles.FYSIKOS)) continue;
            if (ea.getElement().isInstant()) continue;
            result.add(ea);
        }
        return result;
    }

    private static List<Candidate> buildCandidates(ReactionContext context,
                                                   List<ElementalAttachmentInstance> defenders) {
        List<Candidate> candidates = new ArrayList<>();
        for (ElementalAttachmentInstance defender : defenders) {
            GenshinElement defenderMain = defender.getElement().getMainElement();
            GenshinElement attackerMain = context.attackerElement().getMainElement();
            for (ElementalReaction reaction : ModRegistries.ELEMENTAL_REACTIONS_REGISTRY) {
                if (!reaction.canMatch(attackerMain, defenderMain)) continue;
                if (!acceptsReaction(context.targetHost(), context.attackerElement(),
                        defender.getElement(), reaction)) {
                    continue;
                }
                if (reaction.isBlocked(context)) continue;
                int priority = reaction.getBasePriority();
                if (priority < 0) {
                    priority = ReactionPriorityCalculator.computeFor(defender.getElement());
                }
                candidates.add(new Candidate(reaction, defender, priority));
            }
        }
        candidates.sort(Comparator.comparingInt(c -> c.priority));
        return candidates;
    }
    private static boolean acceptsReaction(@Nullable ElementalHost host, GenshinElement attackerElement,
                                           GenshinElement defenderElement,
                                           ElementalReaction reaction) {
        if (host == null) {
            return true;
        }
        return host.acceptsReaction(attackerElement, defenderElement, reaction.getReactionType());
    }

    private static void applyAttackerResidual(ReactionContext context, float remainingQty) {
        if (remainingQty <= 0f) return;
        if (context.attackerFollowsNoResidualRule()) {
            ElementalAttachmentHelper.consume(
                    context.targetContainer(), context.attackerElement(), Float.MAX_VALUE);
        }
    }

    private static class Candidate {
        final ElementalReaction reaction;
        final ElementalAttachmentInstance defender;
        final int priority;

        Candidate(ElementalReaction r, ElementalAttachmentInstance d, int p) {
            this.reaction = r; this.defender = d; this.priority = p;
        }
    }
}
