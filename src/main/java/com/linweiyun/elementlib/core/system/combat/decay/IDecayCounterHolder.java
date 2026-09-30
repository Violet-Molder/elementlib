package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 计时计数器持有者接口。
 */
public interface IDecayCounterHolder {

    /**
     * 获取该实体上的计时计数器管理器，首次调用时自动初始化。
     */
    DecayCounterManager getDecayCounterManager();

    /**
     * 是否已初始化计数器管理器。
     */
    boolean hasDecayCounterManager();
}
