package com.linweiyun.elementlib.core.system.about;

/**
 * 这一次附着相对于宿主身上已有附着做了什么。
 *
 * <ul>
 *   <li>{@link #NEW} —— 之前没有同源实例，新挂了一条；</li>
 *   <li>{@link #REFRESHED} —— 已有同源实例，刷新了来源/时长，附着量没有变大；</li>
 *   <li>{@link #OVERWRITTEN} —— 已有实例，这次的量更大，按强附着覆盖了它。</li>
 * </ul>
 */
public enum AttachWrite {
    NEW,
    REFRESHED,
    OVERWRITTEN
}