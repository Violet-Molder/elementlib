package com.linweiyun.elementlib.core.module;

/**
 * 模块数据的生命周期策略。
 */
public enum ElibModulePersistence {

    /** 写存档；宿主卸载再加载后数据还在。 */
    PERSISTENT,

    /**
     * 不写存档；宿主卸载即丢，重新加载后懒建为初始值。
     *
     * <p>方块韧性用它：「卸载重载后恢复满值」不需要任何恢复逻辑。
     */
    TRANSIENT_PER_LOAD
}
