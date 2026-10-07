package com.linweiyun.elementlib.api.event;

import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 事件广播与注册入口。
 *
 * <p>事件走 NeoForge 的事件总线（{@code NeoForge.EVENT_BUS}）：{@link #post} 广播，{@link #register} 注册监听者。
 * {@link #post} 会收住监听者抛出的异常并记日志（NeoForge 的 EventBus 会重抛），保证调用点继续执行。
 */
public final class ElibEvents {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    /** 统一的事件 logger（按系统）：默认用 elementlib 自己的 EVENT 组。 */
    private static final Logger DEFAULT_EVENT_LOGGER = ModLog.getLogger(LogGroup.EVENT);

    private static volatile Logger eventLogger = DEFAULT_EVENT_LOGGER;

    private ElibEvents() {
    }

    /** 换掉统一的事件 logger（使用方可以接自己的 EVENT 组）；传 {@code null} 恢复默认。 */
    public static void setEventLogger(@Nullable Logger logger) {
        eventLogger = logger == null ? DEFAULT_EVENT_LOGGER : logger;
    }

    /**
     * 广播一次事件；监听者抛出异常时记日志并吞掉。
     *
     * @return 事件本身
     */
    public static <T extends Event> T post(T event) {
        if (event == null) {
            return null;
        }
        Logger logger = eventLogger;
        if (logger.isDebugEnabled()) {
            logger.debug("[事件] {}", ElibIdentifiedEvent.describe(event));
        }
        try {
            NeoForge.EVENT_BUS.post(event);
        } catch (Throwable t) {
            LOGGER.error("[ElementLib] 事件广播中有监听者抛出异常，已隔离 event={}",
                    event.getClass().getName(), t);
        }
        return event;
    }

    /** 注册监听者（实例或 Class，规则与 NeoForge 一致）。 */
    public static void register(Object listener) {
        if (listener != null) {
            NeoForge.EVENT_BUS.register(listener);
        }
    }

    /** 注销监听者。 */
    public static void unregister(Object listener) {
        if (listener != null) {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }
}