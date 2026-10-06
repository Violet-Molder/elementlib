package com.linweiyun.elementlib.api;

/**
 * 攻击监听器 —— 攻击管线的扩展点。
 *
 * <p>元素附着由管线内部先做完，监听器拿到 outcome 时已经能读到每个宿主的 {@code AttachResult}。
 * 方块韧性、表现、资源消耗都从这里接入。
 */
@FunctionalInterface
public interface ElibAttackListener {

    void onAttack(ElibAttackOutcome outcome);
}
