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
 */
public class ElementalAttachmentHelper {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile) {
        return attach(host, element, source, profile, AttachContext.ENVIRONMENT);
    }
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      AttachContext context) {
        return doAttach(host, element, source, profile, context, true);
    }
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      @Nullable String sourceKey, long gameTime) {
        return attach(host, element, source, profile,
                new AttachContext(sourceKey, gameTime, null, null));
    }
    public static AttachResult attachInternal(ElementalHost host, GenshinElement element,
                                              AttachmentSource source, AttachmentProfile profile) {
        return doAttach(host, element, source, profile, AttachContext.ENVIRONMENT, false);
    }

    public static AttachResult attachInternal(ElementalHost host, GenshinElement element,
                                              AttachmentSource source, AttachmentProfile profile,
                                              AttachContext context) {
        return doAttach(host, element, source, profile, context, false);
    }
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
    /** 瞬发元素（风/岩）的衰减速率：不按常规公式，快速消失。 */
    private static final float INSTANT_DECAY_PER_SECOND = 1.0f;

    /** 瞬发元素的存续时长（秒）。 */
    private static final float INSTANT_DURATION_SECONDS = 0.5f;

    /** 把常规附着参数换成瞬发用的短命参数，只改衰减与时长。 */
    private static AttachmentProfile instantProfile(AttachmentProfile base) {
        return new AttachmentProfile(base.getBaseQuantity(), base.getLossMultiplier(),
                INSTANT_DECAY_PER_SECOND, INSTANT_DURATION_SECONDS);
    }
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
        if (host != null && !host.acceptsElement(element, source, profile)) {
            return AttachResult.REJECTED;
        }
        if (react) {
            if (element.isInstant()) {
                if (!ElementalReactionManager.canElementReact(element, container, host)) {
                    return AttachResult.REJECTED;
                }
                profile = instantProfile(profile);
            }
        }

        String sourceKey = ctx.sourceKey();
        long gameTime = ctx.gameTime();
        float actualQuantity = profile.actualQuantity();
        ElementalAttachmentInstance existing = findMatching(container, element, source, sourceKey);

        if (existing == null) {
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
                existing.refreshSource(sourceKey, gameTime);
            }
            return finish(host, container, element, source, profile, ctx, react, actualQuantity);
        }

        if (actualQuantity <= existing.getUnit()) {
            if (sourceKey != null) {
                existing.refreshSource(sourceKey, gameTime);
            }
            return finish(host, container, element, source, profile, ctx, react, actualQuantity);
        }
        existing.refreshQuantity(actualQuantity);
        if (sourceKey != null) {
            existing.refreshSource(sourceKey, gameTime);
        }
        if (element.canOverrideDecay()) {
            existing.overrideDecayRate(profile.getDecayPerSecond());
        }
        return finish(host, container, element, source, profile, ctx, react, actualQuantity);
    }
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
                ctx.sourceKey(),
                ctx.attackerEntity(),
                container, host == null ? null : host.entity(), host);
        return new AttachResult(true, ElementalReactionManager.tryReactFor(host, reactionContext));
    }
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
