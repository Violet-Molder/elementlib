package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 元素状态每 tick 之后的回调（容器已 tick、寒已同步）。
 */
@FunctionalInterface
public interface ElementalTickListener {

    /**
     * @param frozenWithCold 该实体此刻「既有寒又有冰/冻」——冻结效果真正成立
     */
    void afterElementalTick(LivingEntity entity, StatusContainer container, boolean frozenWithCold);
}
