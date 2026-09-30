package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.api.ReactionCategory;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
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
 *
 * <p>附着入口调用 {@link #tryReactFor(ElementalHost, ReactionContext)}：收集目标身上的先手元素，
 * 配出「后手元素 + 某个先手元素」能触发的全部反应，按优先级从小到大依次执行，
 * 每轮扣减后手量，直到后手耗尽。反应反馈由本类统一发出。
 */
public class ElementalReactionManager {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    public static boolean canElementReact(GenshinElement attackerElement,
                                          StatusContainer container) {
        return canElementReact(attackerElement, container, null);
    }

    /**
     * 同 {@link #canElementReact(GenshinElement, StatusContainer)}，但把宿主的筛查也算进去。
     *
     * <p>瞬发元素（风 / 岩）只为「触发一次反应」而来：宿主拒绝了它全部候选反应时，
     * 这次附着就不该发生。
     *
     * @param host 目标宿主；{@code null} 表示没有宿主信息，视为允许全部反应
     */
    public static boolean canElementReact(GenshinElement attackerElement,
                                          StatusContainer container,
                                          @Nullable ElementalHost host) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        if (attackerMain == null) return false;

        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement().isInstant()) continue;
            if (ModElements.is(ea.getElement(), ModElements.FYSIKOS)) continue;

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

    // ==================== 反应入口 ====================

    /**
     * 宿主感知的反应入口 —— 任何来源（攻击、环境、自身、反应内二次写入）触发的反应都走这里；
     * 反应反馈按宿主落在方块位置或实体锚点上。
     */
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

    // ==================== 内部 ====================

    /** 反应反馈出口 —— 实体端与方块端的唯一差异。 */
    @FunctionalInterface
    private interface ReactionIndicator {
        void show(ElementalReactionType reactionType);
    }

    /**
     * 反应统一执行体 —— 实体端与方块端共用。
     */
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
                    context.attackerEntity(),
                    context.targetContainer(),
                    context.targetEntity(),
                    context.targetHost()
            );

            ReactionResult result = cand.reaction.execute(roundContext);

            // 反应实现必须返回结果；null 当作「没发生」。
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

    /**
     * 月感电与星体系的字由各自的伤害链出，反应层不发；其余分类照常发。
     */
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
            if (ModElements.is(ea.getElement(), ModElements.FYSIKOS)) continue;
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
                // 第二段筛查：宿主收不收这个反应。被拒绝 = 这次不反应、先手元素留在身上。
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

    /**
     * 问宿主「这个反应能不能发生在你身上」，最终落到实体实现的 {@code ElementalAttachable.onReactElement}。
     *
     * @param attackerElement 后手（本次附着）元素
     * @param defenderElement 先手（宿主身上已有）元素
     */
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
