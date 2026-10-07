package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 环境元素附着（水 / 火）的宿主出口 —— 决定这份附着挂到谁身上。
 *
 * <p>默认实现是实体自己；返回 {@code null} 表示这次环境附着不进行。
 */
@FunctionalInterface
public interface EnvironmentAttachTarget {

    @Nullable
    ElementalHost targetFor(LivingEntity entity);
}
