package com.linweiyun.elementlib.util.log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;
import org.slf4j.spi.NOPLoggingEventBuilder;

/**
 * 带闸门的 logger：真正干活的是 {@code delegate}，本类只负责在总开关或组开关关掉时把调用吃掉。
 */
final class GroupLogger implements Logger {

    private final Logger delegate;
    private final LogGroup group;

    GroupLogger(Logger delegate, LogGroup group) {
        this.delegate = delegate;
        this.group = group;
    }

    private boolean muted() {
        return !ModLog.isEnabled(group);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return !muted() && delegate.isTraceEnabled();
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return !muted() && delegate.isTraceEnabled(marker);
    }

    @Override
    public void trace(String msg) {
        if (!muted()) delegate.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        if (!muted()) delegate.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (!muted()) delegate.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (!muted()) delegate.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (!muted()) delegate.trace(msg, t);
    }

    @Override
    public void trace(Marker marker, String msg) {
        if (!muted()) delegate.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        if (!muted()) delegate.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        if (!muted()) delegate.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        if (!muted()) delegate.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        if (!muted()) delegate.trace(marker, msg, t);
    }

    // ===================== DEBUG =====================

    @Override
    public boolean isDebugEnabled() {
        return !muted() && delegate.isDebugEnabled();
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return !muted() && delegate.isDebugEnabled(marker);
    }

    @Override
    public void debug(String msg) {
        if (!muted()) delegate.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        if (!muted()) delegate.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (!muted()) delegate.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (!muted()) delegate.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (!muted()) delegate.debug(msg, t);
    }

    @Override
    public void debug(Marker marker, String msg) {
        if (!muted()) delegate.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        if (!muted()) delegate.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        if (!muted()) delegate.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        if (!muted()) delegate.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        if (!muted()) delegate.debug(marker, msg, t);
    }

    // ===================== INFO =====================

    @Override
    public boolean isInfoEnabled() {
        return !muted() && delegate.isInfoEnabled();
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return !muted() && delegate.isInfoEnabled(marker);
    }

    @Override
    public void info(String msg) {
        if (!muted()) delegate.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        if (!muted()) delegate.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (!muted()) delegate.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        if (!muted()) delegate.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        if (!muted()) delegate.info(msg, t);
    }

    @Override
    public void info(Marker marker, String msg) {
        if (!muted()) delegate.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        if (!muted()) delegate.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        if (!muted()) delegate.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        if (!muted()) delegate.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        if (!muted()) delegate.info(marker, msg, t);
    }

    // ===================== WARN =====================

    @Override
    public boolean isWarnEnabled() {
        return !muted() && delegate.isWarnEnabled();
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return !muted() && delegate.isWarnEnabled(marker);
    }

    @Override
    public void warn(String msg) {
        if (!muted()) delegate.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        if (!muted()) delegate.warn(format, arg);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (!muted()) delegate.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (!muted()) delegate.warn(format, arguments);
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (!muted()) delegate.warn(msg, t);
    }

    @Override
    public void warn(Marker marker, String msg) {
        if (!muted()) delegate.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        if (!muted()) delegate.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        if (!muted()) delegate.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        if (!muted()) delegate.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        if (!muted()) delegate.warn(marker, msg, t);
    }

    // ===================== ERROR =====================

    @Override
    public boolean isErrorEnabled() {
        return !muted() && delegate.isErrorEnabled();
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return !muted() && delegate.isErrorEnabled(marker);
    }

    @Override
    public void error(String msg) {
        if (!muted()) delegate.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        if (!muted()) delegate.error(format, arg);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (!muted()) delegate.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        if (!muted()) delegate.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        if (!muted()) delegate.error(msg, t);
    }

    @Override
    public void error(Marker marker, String msg) {
        if (!muted()) delegate.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        if (!muted()) delegate.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        if (!muted()) delegate.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        if (!muted()) delegate.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        if (!muted()) delegate.error(marker, msg, t);
    }

    // ===================== 流式（atInfo().log(...) 那一套） =====================

    @Override
    public boolean isEnabledForLevel(Level level) {
        return !muted() && delegate.isEnabledForLevel(level);
    }

    @Override
    public LoggingEventBuilder atLevel(Level level) {
        return muted() ? NOPLoggingEventBuilder.singleton() : delegate.atLevel(level);
    }

    @Override
    public LoggingEventBuilder makeLoggingEventBuilder(Level level) {
        return muted() ? NOPLoggingEventBuilder.singleton() : delegate.makeLoggingEventBuilder(level);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
