package com.linweiyun.elementlib.core.system.combat.decay;

import net.minecraft.server.level.ServerLevel;

/**
 * 计时计数器服务 - 管理Worker线程的启动/停止。
 *
 * <p>服务器启动时调用 {@link #initOnServer}，关闭时调用 {@link #shutdown}。
 */
public class DecayCounterService {

    private static volatile boolean initialized = false;

    /**
     * 在服务器级别启动Worker（通常在ServerTickEvent或Level加载事件中调用）。
     */
    public static void initOnServer(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        DecayCounterWorker.getInstance().start(() -> {
            // 通过Level获取当前tick
            return level.getGameTime();
        });
    }

    /**
     * 停止Worker。
     */
    public static void shutdown() {
        if (!initialized) return;
        initialized = false;
        DecayCounterWorker.getInstance().stop();
    }

    /**
     * 是否已初始化。
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
