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

    boolean acceptsElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);

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
