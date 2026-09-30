package com.linweiyun.elementlib.core.system.about.host;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.api.ElementalReactionType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>可附着宿主</b> —— 「元素能挂在什么东西上」的唯一抽象：生物、方块（以及以后的物品）都实现它，
 * 附着与反应只认宿主。
 *
 * <pre>
 *   附着 = ElementalAttachmentHelper.attach(host, element, source, profile)
 *        → host.acceptsElement(...)   ← 第一段筛查：这次附着收不收
 *        → 写进 host.container()
 *        → ElementalReactionManager     ← 第二段筛查：host.acceptsReaction(...)
 * </pre>
 *
 * <p><b>实现者只需回答四件事</b>：容器在哪（{@link #container()}）、
 * 收不收这次附着（{@link #acceptsElement}）、收不收某个反应（{@link #acceptsReaction}）、
 * 附着/分离时要做什么（{@link #onElementAttached} / {@link #onElementDetached}）。
 */
public interface ElementalHost {

    /** 宿主是否还有效（实体活着 / 方块还在加载范围内）。无效的宿主不再参与附着与反应。 */
    boolean isValid();

    /**
     * 宿主的状态容器（可写）。实现方负责把改动落回自己的存储；无效宿主返回 {@code null}。
     */
    @Nullable
    StatusContainer container();

    /** 宿主标识，用于日志与去重。 */
    String hostKey();

    /**
     * <b>第一段筛查</b>：这次附着收不收。
     *
     * <p>这是「能被什么附着」的表达处：元素生物可以只收自己那一种、冰史莱姆可以拒绝水、
     * 水方块只收冰。返回 {@code false} 时附着与随之而来的反应都不会发生。
     */
    boolean acceptsElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);

    /**
     * <b>第二段筛查</b>：某个反应能不能在这个宿主上发生。
     *
     * <p>用于「允许挂水但不接受冻结 → 只冰水共存、不生成冻元素」。默认全允许；
     * 返回 {@code false} 时该反应被跳过（先手元素保留，形成共存）。
     *
     * @param attackerElement 后手（本次附着）元素
     * @param defenderElement 先手（宿主身上已有）元素
     */
    default boolean acceptsReaction(GenshinElement attackerElement,
                                    GenshinElement defenderElement,
                                    ElementalReactionType reactionType) {
        return true;
    }

    /** 附着成功后的宿主侧效果。 */
    void onElementAttached(GenshinElement element);

    /** 附着被移除后的宿主侧效果（撤销 {@link #onElementAttached} 做的事）。 */
    void onElementDetached(GenshinElement element);

    // ==================== 载体视图（元素钩子按需取用，方块宿主返回 null） ====================

    /** 若宿主是生物则返回其实体，否则 {@code null}。 */
    @Nullable
    default LivingEntity entity() {
        return null;
    }

    /**
     * <b>反应飘字的落点</b> —— 默认就是宿主实体本身。
     * 和 {@link #entity()} 分开：那个是「元素钩子把效果挂在谁身上」，这个只回答「反应在哪出字」。
     */
    @Nullable
    default LivingEntity indicatorAnchor() {
        return entity();
    }

    /** 若宿主是方块则返回其所在世界，否则 {@code null}。 */
    @Nullable
    default ServerLevel level() {
        return null;
    }

    /** 若宿主是方块则返回其坐标，否则 {@code null}。 */
    @Nullable
    default BlockPos blockPos() {
        return null;
    }
}
