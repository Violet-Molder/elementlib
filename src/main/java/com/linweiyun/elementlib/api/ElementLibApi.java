package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachContext;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.attack.ElibAttackPipeline;
import com.linweiyun.elementlib.core.system.attack.ElibDefaultVanillaAttackBridge;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.damage.VariantGateHolder;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * ElementLib 对外门面：附着、消耗、查询，以及伤害处理器 / 变体门 / 图标总闸三个扩展点。
 */
public final class ElementLibApi {

    private static volatile boolean auraIconVisible = true;

    private static volatile ReactionFeedbackHandler feedbackHandler;

    private static volatile ElementalTickListener tickListener;

    private static volatile EnvironmentAttachTarget environmentTarget;

    private ElementLibApi() {
    }

    // ==================== 附着 ====================

    /**
     * 给宿主挂元素（无上下文），并在附着内部尝试反应。
     *
     * @return 附着结果；参数为空或宿主拒收时返回 {@link AttachResult#REJECTED}
     */
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile) {
        return attach(host, element, source, profile, AttachContext.ENVIRONMENT);
    }

    /** 给宿主挂元素（带上下文，攻击型附着用 {@link AttachContext#attack}）。 */
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile, AttachContext ctx) {
        if (host == null || element == null || source == null || profile == null) {
            return AttachResult.REJECTED;
        }
        return ElementalAttachmentHelper.attach(host, element, source, profile, ctx);
    }

    /** 给生物实体挂元素，容器即实体身上的状态容器。 */
    public static AttachResult attach(LivingEntity target, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile) {
        if (target == null || element == null || source == null || profile == null) {
            return AttachResult.REJECTED;
        }
        return ElementalAttachmentHelper.attach(EntityHost.of(target), element, source, profile);
    }

    /**
     * 从容器里消耗指定元素的附着量。
     *
     * @return 实际消耗量；容器为空时为 {@code 0}
     */
    public static float consume(StatusContainer container, GenshinElement element, float amount) {
        return ElementalAttachmentHelper.consume(container, element, amount);
    }

    // ==================== 查询 ====================

    /**
     * 生物身上的元素状态容器。
     *
     * @return 容器；{@code entity} 为 {@code null} 时返回 {@code null}
     */
    public static StatusContainer container(LivingEntity entity) {
        return entity == null ? null : ElementalAttachments.container(entity);
    }

    /**
     * 按 id 查元素。
     *
     * @return 元素；未注册或 {@code id} 为 {@code null} 时返回 {@code null}
     */
    @Nullable
    public static GenshinElement element(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return ModRegistries.ELEMENT_REGISTRY.get(id);
    }

    /**
     * 按 id 查反应类型。
     *
     * @return 反应类型；未注册或 {@code id} 为 {@code null} 时返回 {@code null}
     */
    @Nullable
    public static ElementalReactionType reactionType(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        return ModRegistries.REACTION_TYPE_REGISTRY.get(id);
    }

    /** 已注册的全部反应（快照，不可修改）。 */
    public static List<ElementalReaction> reactions() {
        return StreamSupport.stream(ModRegistries.ELEMENTAL_REACTIONS_REGISTRY.spliterator(), false).toList();
    }

    /** 已注册的全部反应类型（快照，不可修改）。 */
    public static List<ElementalReactionType> reactionTypes() {
        return StreamSupport.stream(ModRegistries.REACTION_TYPE_REGISTRY.spliterator(), false).toList();
    }

    // ==================== 扩展点 ====================

    /**
     * 换掉伤害处理器。
     *
     * @param handler 新处理器；传 {@code null} 恢复默认处理器
     */
    public static void setDamageHandler(ElementalDamageHandler handler) {
        VariantGateHolder.setDamageHandler(handler);
    }

    /** 当前的伤害处理器，永不为 {@code null}。 */
    public static ElementalDamageHandler damageHandler() {
        return VariantGateHolder.damageHandler();
    }

    /**
     * 换掉星体系 / 月感电的变体门。
     *
     * @param gate 新门；传 {@code null} 恢复读 {@link DemoContentToggles} 的默认门
     */
    public static void setVariantGate(ReactionVariantGate gate) {
        VariantGateHolder.setGate(gate);
    }

    /** 当前的变体门，永不为 {@code null}。 */
    public static ReactionVariantGate variantGate() {
        return VariantGateHolder.gate();
    }

    /** 元素附着图标总闸；关掉后不显示任何附着图标。 */
    public static void setAuraIconVisible(boolean visible) {
        auraIconVisible = visible;
    }

    /** 元素附着图标总闸当前状态。 */
    public static boolean auraIconVisible() {
        return auraIconVisible;
    }

    /** 换掉反应表现出口；传 {@code null} 恢复库自带的粒子 + 文案。 */
    public static void setFeedbackHandler(@Nullable ReactionFeedbackHandler handler) {
        feedbackHandler = handler;
    }

    /** 当前的反应表现出口；未替换时为 {@code null}。 */
    @Nullable
    public static ReactionFeedbackHandler feedbackHandler() {
        return feedbackHandler;
    }

    /** 元素状态每 tick 之后的回调；传 {@code null} 清除。 */
    public static void setTickListener(@Nullable ElementalTickListener listener) {
        tickListener = listener;
    }

    @Nullable
    public static ElementalTickListener tickListener() {
        return tickListener;
    }

    /** 环境附着的宿主出口；传 {@code null} 恢复「挂到实体自己」。 */
    public static void setEnvironmentTarget(@Nullable EnvironmentAttachTarget target) {
        environmentTarget = target;
    }

    @Nullable
    public static EnvironmentAttachTarget environmentTarget() {
        return environmentTarget;
    }

    // ==================== 攻击 ====================

    /**
     * 统一攻击入口：对空 / 方块左键 / 实体左键 / 动作伤害点都走这里。
     *
     * @return 这次攻击触及的宿主与每个宿主的附着结果
     */
    public static ElibAttackOutcome attack(ElibAttackAction action) {
        return ElibAttackPipeline.dispatch(action);
    }

    /** 注册攻击监听器（方块韧性、表现、消耗等）。 */
    public static void addAttackListener(ElibAttackListener listener) {
        ElibAttackPipeline.addListener(listener);
    }

    /** 移除攻击监听器。 */
    public static void removeAttackListener(ElibAttackListener listener) {
        ElibAttackPipeline.removeListener(listener);
    }

    /** 攻击门禁；MineGenshin 用它注入「仅原神模式」，传 {@code null} 恢复永远放行。 */
    public static void setAttackGate(@Nullable ElibAttackGate gate) {
        ElibAttackPipeline.setGate(gate);
    }

    /** 元素解析器；传 {@code null} 表示只有显式带元素的攻击才附着。 */
    public static void setAttackElementResolver(@Nullable ElibAttackElementResolver resolver) {
        ElibAttackPipeline.setElementResolver(resolver);
    }

    /** 注册方块关注点：方块韧性这类"只看某些方块"的模块用它。 */
    public static void addAttackBlockInterest(ElibAttackBlockInterest interest) {
        ElibAttackPipeline.addBlockInterest(interest);
    }

    /** 注册外部宿主工厂（例如 PGCharacter）。 */
    public static <C> void registerModuleCarrier(Class<C> carrierType,
                                                 java.util.function.Function<C, com.linweiyun.elementlib.core.module.ElibModuleHost> factory) {
        com.linweiyun.elementlib.core.module.ElibModuleHosts.registerCarrier(carrierType, factory);
    }

    /** 原版实体攻击默认桥开关；MineGenshin 关掉它，避免与自己的动作系统重复。 */
    public static void setDefaultVanillaAttackBridge(boolean enabled) {
        ElibDefaultVanillaAttackBridge.setEnabled(enabled);
    }
}
