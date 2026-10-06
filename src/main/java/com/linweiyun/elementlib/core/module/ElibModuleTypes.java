package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.attachment.StatusContainer;

/**
 * elementlib 内置的模块类型。
 *
 * <p>本库只内置「元素」一个模块；韧性等由使用方自行注册。
 */
public final class ElibModuleTypes {

    /**
     * 元素模块 —— 数据对象就是现有的 {@link StatusContainer}。
     *
     * <p>宿主种类默认支持 entity / block / item；character 由 MineGenshin 在启动时
     * 用 {@link ElibModuleType#support} 追加，lib 不需要知道它。
     */
    public static final ElibModuleType<StatusContainer> ELEMENT = ElibModuleRegistry.register(
            ElibModuleType.builder(ElementLib.id("element"), StatusContainer.class)
                    .supports(ElibModuleTargetKinds.ENTITY,
                            ElibModuleTargetKinds.BLOCK,
                            ElibModuleTargetKinds.ITEM)
                    .persistent()
                    .sync(ElibModuleSync.SYNC_TO_CLIENT)
                    .factory(StatusContainer::new)
                    .codec(StatusContainer.CODEC)
                    .streamCodec(StatusContainer.STREAM_CODEC)
                    .build());

    private ElibModuleTypes() {
    }

    /** 触发类初始化（注册表需要 ELEMENT 已登记时调用）。 */
    public static void init() {
    }
}
