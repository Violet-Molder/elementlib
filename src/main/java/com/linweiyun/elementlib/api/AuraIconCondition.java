package com.linweiyun.elementlib.api;

/**
 * 一条图标显示条件：返回 {@code false} 表示这次附着不画图标。
 *
 * <p>多条条件之间取「与」。
 */
@FunctionalInterface
public interface AuraIconCondition {

    boolean shouldShow(AuraIconContext ctx);
}
