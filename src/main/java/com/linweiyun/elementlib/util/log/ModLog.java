package com.linweiyun.elementlib.util.log;

import com.linweiyun.elementlib.ElementLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本模组的 logger 工厂：所有 {@code LOGGER} 字段都从这里拿。
 */
public final class ModLog {

    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private ModLog() {
    }

    public static Logger getLogger(LogGroup group) {
        return new GroupLogger(LoggerFactory.getLogger(callerClass()), group);
    }

    public static boolean isEnabled(LogGroup group) {
        return ElementLib.LOG_ENABLED && group.isEnabled();
    }

    public static void setEnabled(LogGroup group, boolean enabled) {
        group.setEnabled(enabled);
    }

    public static void enableOnly(LogGroup group) {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(g == group);
        }
    }

    public static void enableAllGroups() {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(true);
        }
    }

    public static void disableAllGroups() {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(false);
        }
    }

    private static Class<?> callerClass() {
        return WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(c -> c != ModLog.class)
                .findFirst()
                .orElse(ModLog.class));
    }
}
