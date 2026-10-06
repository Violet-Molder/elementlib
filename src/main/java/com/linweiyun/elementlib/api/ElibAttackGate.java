package com.linweiyun.elementlib.api;

/**
 * 攻击门禁 —— 返回 {@code false} 时这次攻击不进入管线。
 *
 * <p>MineGenshin 用它注入「仅原神模式」；elementlib 独立运行默认恒真。
 */
@FunctionalInterface
public interface ElibAttackGate {

    boolean test(ElibAttackAction action);
}
