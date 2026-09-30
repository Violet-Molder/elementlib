package com.linweiyun.elementlib.util.log;

import com.linweiyun.elementlib.ElementLib;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本模组的 logger 工厂：所有 {@code LOGGER} 字段都从这里拿。
 *
 * <p>名字照旧取自调用者类（{@link #getLogger} 自己走一遍栈帧跳过本类），外层再套一个
 * {@link GroupLogger} 做闸门，所以日志里显示的类名与从前一致。一次日志要出声必须同时满足
 * {@link ElementLib#LOG_ENABLED}（总开关）与 {@link LogGroup#isEnabled()}（组开关）；
 * 开关是<b>每次调用现查</b>的，运行期改立刻生效：{@code LogGroup.COMBAT.setEnabled(false)} 只静音一组，
 * {@link #enableOnly(LogGroup)} 只留一组。闸门关掉时连 {@code isInfoEnabled()} 也返回 {@code false}，
 * 调用点「先问再拼」的写法会连字符串拼装都省掉。本类不碰 Minecraft 类型，混入插件也能安全使用。
 */
public final class ModLog {

    /** 找调用者类用。{@code RETAIN_CLASS_REFERENCE} 才能拿到 Class 而不是字符串 */
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private ModLog() {
    }

    /**
     * 给声明它的那个类拿一个 logger，写法固定为字段初始化那一行：
     * {@code private static final Logger LOGGER = ModLog.getLogger(LogGroup.COMBAT);}
     *
     * @param group 这条日志属于哪一组，决定它跟哪个分组开关走
     */
    public static Logger getLogger(LogGroup group) {
        return new GroupLogger(LoggerFactory.getLogger(callerClass()), group);
    }

    /** 这一组现在会不会出声（总开关 + 组开关） */
    public static boolean isEnabled(LogGroup group) {
        return ElementLib.LOG_ENABLED && group.isEnabled();
    }

    /** 单独开关一组 */
    public static void setEnabled(LogGroup group, boolean enabled) {
        group.setEnabled(enabled);
    }

    /** 只留一组出声，其余全部静音（总开关保持不动） */
    public static void enableOnly(LogGroup group) {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(g == group);
        }
    }

    /** 所有组都打开（总开关保持不动） */
    public static void enableAllGroups() {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(true);
        }
    }

    /** 所有组都静音（等价于只关总开关，但保留各自原来的开关状态） */
    public static void disableAllGroups() {
        for (LogGroup g : LogGroup.values()) {
            g.setEnabled(false);
        }
    }

    /**
     * 调用 {@link #getLogger} 的那个类：栈里第一帧是本方法自己（可能还有一层 lambda），跳过本类后即调用者。
     * 取不到时退回本类，宁可名字难看也不抛异常（日志工厂抛异常会带崩调用方的类初始化）。
     */
    private static Class<?> callerClass() {
        return WALKER.walk(frames -> frames
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(c -> c != ModLog.class)
                .findFirst()
                .orElse(ModLog.class));
    }
}
