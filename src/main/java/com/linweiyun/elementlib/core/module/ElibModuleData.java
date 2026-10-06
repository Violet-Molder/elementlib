package com.linweiyun.elementlib.core.module;

import net.minecraft.resources.Identifier;

/**
 * <b>模块数据</b> —— 挂在宿主身上的一份注册项实例。
 *
 * <p>elementlib 的内置实现是元素（{@code StatusContainer}）；MineGenshin 的实现是韧性。
 */
public interface ElibModuleData {

    /** 这份数据属于哪个模块类型。 */
    ElibModuleType<?> type();

    default Identifier typeId() {
        return type().id();
    }

    /** 首次挂到这个宿主上时调用。 */
    default void onAttach(ElibModuleHost host) {
    }

    /** 从宿主上移除时调用。 */
    default void onDetach(ElibModuleHost host) {
    }

    /** 宿主每 tick 推进；由各宿主自己的 tick 入口调用。 */
    default void tick(ElibModuleHost host) {
    }
}
