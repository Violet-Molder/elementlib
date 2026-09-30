package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.reaction.ElementalReactionManager;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Objects;

/**
 * <b>元素附着的唯一入口</b> —— 「附着 → 附着内反应 → 反应引发效果」这一整条链都在这里。
 *
 * <p>调用方只需要说「谁、什么元素、什么来源、多少量」；宿主筛查、覆盖规则、损耗、常驻补量、
 * 反应、后手残留全部由本入口负责。只要经过这里，反应一定会被尝试（环境附着与攻击附着同一体系）。
 *
 * <pre>
 * attach(host, element, source, profile[, ctx])           ← 会触发反应（默认）
 * attachInternal(host, element, source, profile[, ctx])   ← 反应内部的二次写入用，不再触发反应（防递归）
 * </pre>
 */
public class ElementalAttachmentHelper {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    // ========== 附着入口 —— 唯一入口是「宿主」 ==========

    /** 不带上下文的附着（环境、自身、反应内）。 */
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile) {
        return attach(host, element, source, profile, AttachContext.ENVIRONMENT);
    }

    /** 带上下文的附着（攻击型附着用 {@link AttachContext#attack}）。 */
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      AttachContext context) {
        return doAttach(host, element, source, profile, context, true);
    }

    /** 便捷重载：带来源标识与附着时刻（用于同源附着匹配）。 */
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      @Nullable String sourceKey, long gameTime) {
        return attach(host, element, source, profile,
                new AttachContext(sourceKey, gameTime, null, null));
    }

    /**
     * <b>反应内部的二次写入</b> —— 例如冻结反应生成冻元素、扩散把元素带到旁边的人身上。
     * 只负责把附着写进去，不再触发反应（否则会形成「反应生附着、附着再反应」的递归）。
     */
    public static AttachResult attachInternal(ElementalHost host, GenshinElement element,
                                              AttachmentSource source, AttachmentProfile profile) {
        return doAttach(host, element, source, profile, AttachContext.ENVIRONMENT, false);
    }

    public static AttachResult attachInternal(ElementalHost host, GenshinElement element,
                                              AttachmentSource source, AttachmentProfile profile,
                                              AttachContext context) {
        return doAttach(host, element, source, profile, context, false);
    }

    /**
     * 反应内部/自附着的写入，用<b>调用方已经拿到的容器</b>，不再回头问宿主（避免递归）。
     *
     * @param container 已解析好的容器（通常是 {@code host.container()} 的结果）
     */
    public static AttachResult attachInternalTo(StatusContainer container, ElementalHost host,
                                                GenshinElement element, AttachmentSource source,
                                                AttachmentProfile profile) {
        return doAttach(container, host, element, source, profile, AttachContext.ENVIRONMENT, false);
    }

    /**
     * 生物附着便捷重载 —— 生物宿主 + 实体自带容器。
     * 若容器不在这个实体上，请改用带容器的通用入口。
     */
    public static AttachResult attach(LivingEntity target, StatusContainer container,
                                      GenshinElement element,
                                      AttachmentSource source,
                                      AttachmentProfile profile) {
        return doAttach(container, EntityHost.of(target), element, source, profile,
                AttachContext.ENVIRONMENT, true);
    }

    /** 带来源标识的生物附着重载。 */
    public static AttachResult attach(LivingEntity target, StatusContainer container,
                                      GenshinElement element,
                                      AttachmentSource source,
                                      AttachmentProfile profile,
                                      @Nullable String sourceKey, long gameTime) {
        return doAttach(container, EntityHost.of(target), element, source, profile,
                new AttachContext(sourceKey, gameTime, null, null), true);
    }

    // ========== 消耗（元素反应调用）==========

    /** 从容器里消耗指定元素的附着量，返回实际消耗量。 */
    public static float consume(StatusContainer container, GenshinElement element, float amount) {
        if (container == null) return 0f;
        return consumeInternal(container, element, amount);
    }

    private static float consumeInternal(StatusContainer container, GenshinElement element, float amount) {
        float remaining = amount;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (ea.getElement() != element) continue;
            float consumed = ea.consume(remaining);
            remaining -= consumed;
            if (remaining <= 0) break;
        }
        return amount - remaining;
    }

    // ========== 核心附着逻辑 ==========

    /** 瞬发元素（风/岩）的衰减速率：不按常规公式，快速消失。 */
    private static final float INSTANT_DECAY_PER_SECOND = 1.0f;

    /** 瞬发元素的存续时长（秒）。 */
    private static final float INSTANT_DURATION_SECONDS = 0.5f;

    /** 把常规附着参数换成瞬发用的短命参数，只改衰减与时长。 */
    private static AttachmentProfile instantProfile(AttachmentProfile base) {
        return new AttachmentProfile(base.getBaseQuantity(), base.getLossMultiplier(),
                INSTANT_DECAY_PER_SECOND, INSTANT_DURATION_SECONDS);
    }

    /**
     * 写附着 + （可选）触发反应。
     *
     * <p>顺序固定：宿主筛查 → 写进容器 → 触发反应。宿主拒收时什么都不写、也不反应
     * ——「没挂上去就没有反应」是附着与反应之间唯一的顺序约束。
     */
    private static AttachResult doAttach(ElementalHost host, GenshinElement element,
                                         AttachmentSource source, AttachmentProfile profile,
                                         AttachContext context, boolean react) {
        if (host == null) {
            return AttachResult.REJECTED;
        }
        AttachContext ctx = context == null ? AttachContext.ENVIRONMENT : context;
        return doAttach(host.container(), host, element, source, profile, ctx, react);
    }

    private static AttachResult doAttach(StatusContainer container, ElementalHost host,
                                         GenshinElement element,
                                         AttachmentSource source,
                                         AttachmentProfile profile,
                                         AttachContext ctx, boolean react) {
        if (container == null) {
            return AttachResult.REJECTED;
        }

        // 0. 先问宿主收不收这次附着
        if (host != null && !host.acceptsElement(element, source, profile)) {
            return AttachResult.REJECTED;
        }

        // 0.5 外部附着（会触发反应的那条路）的两条额外规则：
        //     类元素（冻/激/燃/木）只能由反应内部写入；
        //     瞬发元素（风/岩）只为触发一次反应而来，宿主连一个候选反应都不收时不留附着。
        if (react) {
            if (!element.allowsDirectAttachment()) {
                return AttachResult.REJECTED;
            }
            if (element.isInstant()) {
                if (!ElementalReactionManager.canElementReact(element, container, host)) {
                    return AttachResult.REJECTED;
                }
                profile = instantProfile(profile);
            }
        }

        String sourceKey = ctx.sourceKey();
        long gameTime = ctx.gameTime();

        // 1. 实际附着量 = baseQuantity × lossMultiplier
        float actualQuantity = profile.actualQuantity();

        // 2. 查找容器里"同元素 + 同 source"的已有实例
        ElementalAttachmentInstance existing = findMatching(container, element, source, sourceKey);

        if (existing == null) {
            // 无匹配实例 → 新建
            ElementalAttachmentInstance newInst =
                    new ElementalAttachmentInstance(element, source, profile, actualQuantity);
            if (sourceKey != null) {
                newInst.refreshSource(sourceKey, gameTime);
            } else if (gameTime > 0L) {
                newInst.setAttachTick(gameTime);
            }
            if (host != null) {
                host.onElementAttached(element);
            }
            newInst.setHost(host);
            container.add(newInst);
            return finish(host, container, element, source, profile, ctx, react, actualQuantity);
        }

        // 3. 同来源重复附着：新到期时间更长则延长
        if (sourceKey != null && existing.hasSourceCharacter()) {
            long currentTick = gameTime;
            long existingEnd = existing.getDecayEndTick();
            float newDurationTicks = profile.getDurationSeconds() * 20f;
            long newEnd = currentTick + (long) newDurationTicks;
            if (newEnd > existingEnd) {
                existing.refreshQuantity(actualQuantity);
                existing.overrideDecayRate(profile.getDecayPerSecond());
                existing.setAttachTick(currentTick);
            } else {
                // 支线未超过主线：仅刷新来源信息，不改变到期时间
                existing.refreshSource(sourceKey, gameTime);
            }
            return finish(host, container, element, source, profile, ctx, react, actualQuantity);
        }

        if (actualQuantity <= existing.getUnit()) {
            // 后手段量 ≤ 先手段量 → 不覆盖，但仍记录来源信息
            if (sourceKey != null) {
                existing.refreshSource(sourceKey, gameTime);
            }
            return finish(host, container, element, source, profile, ctx, react, actualQuantity);
        }

        // 4. 发生覆盖
        existing.refreshQuantity(actualQuantity);
        if (sourceKey != null) {
            existing.refreshSource(sourceKey, gameTime);
        }
        if (element.canOverrideDecay()) {
            // 火/激/燃：直接替换为新的衰减速率；其他元素继承原先的（什么都不做）
            existing.overrideDecayRate(profile.getDecayPerSecond());
        }
        return finish(host, container, element, source, profile, ctx, react, actualQuantity);
    }

    /**
     * 附着已写进去之后收尾：触发反应（如果这次附着允许）并返回结果。
     *
     * <p>注意「触发反应」与「覆盖没覆盖」无关：宿主收下了这次附着，就该问一次反应
     * （先手元素可能就是目标身上别的东西），这与既有行为一致。
     */
    private static AttachResult finish(ElementalHost host, StatusContainer container,
                                       GenshinElement element, AttachmentSource source,
                                       AttachmentProfile profile, AttachContext ctx,
                                       boolean react, float actualQuantity) {
        if (!react) {
            return AttachResult.ATTACHED_NO_REACTION;
        }

        float reactionUnit = ctx.reactionUnit() != null ? ctx.reactionUnit() : actualQuantity;
        ReactionContext reactionContext = new ReactionContext(
                element, reactionUnit, source, profile,
                ctx.attackerEntity(),
                container, host == null ? null : host.entity(), host);
        return new AttachResult(true, ElementalReactionManager.tryReactFor(host, reactionContext));
    }

    // ========== 查找工具 ==========

    private static ElementalAttachmentInstance findMatching(
            StatusContainer container, GenshinElement element, AttachmentSource source,
            @Nullable String sourceKey) {
        return (ElementalAttachmentInstance) container.find(inst -> {
            if (inst.isFinished()) return false;
            if (!(inst instanceof ElementalAttachmentInstance ea)) return false;
            if (ea.getElement() != element || ea.getSource() != source) return false;
            if (sourceKey != null && ea.hasSourceCharacter()
                    && !Objects.equals(ea.getSourceCharacterKey(), sourceKey)) {
                return false;
            }
            return true;
        });
    }
}
