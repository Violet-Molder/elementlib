package com.linweiyun.elementlib.core.system.combat.decay;

import lombok.Getter;
import net.minecraft.server.level.ServerLevel;

/**
 * 计时计数器服务 - 管理Worker线程的启动/停止。
 */
public class DecayCounterService {

    @Getter
    private static volatile boolean initialized = false;
    public static void initOnServer(ServerLevel level) {
        if (initialized) return;
        initialized = true;

        DecayCounterWorker.getInstance().start(() -> {
            return level.getGameTime();
        });
    }
    public static void shutdown() {
        if (!initialized) return;
        initialized = false;
        DecayCounterWorker.getInstance().stop();
    }

}
