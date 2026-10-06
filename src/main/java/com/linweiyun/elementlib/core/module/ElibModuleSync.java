package com.linweiyun.elementlib.core.module;

/**
 * 模块数据的客户端同步策略。
 */
public enum ElibModuleSync {

    /** 不同步。 */
    NONE,

    /** 随宿主附件同步给客户端（元素图标、韧性条需要）。 */
    SYNC_TO_CLIENT
}
