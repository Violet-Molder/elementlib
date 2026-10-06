package com.linweiyun.elementlib.core.module;

import net.minecraft.resources.Identifier;

/**
 * <b>模块宿主种类</b> —— 「这个模块能挂在什么东西上」的注册项。
 *
 * <p>用注册项而不是 Java 枚举：elementlib 内置 {@code entity / block / item}，
 * MineGenshin 可以额外注册 {@code minegenshin:character}，库本身不需要知道它。
 */
public final class ElibModuleTargetKind {

    private final Identifier id;

    ElibModuleTargetKind(Identifier id) {
        this.id = id;
    }

    public Identifier id() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
