package com.linweiyun.elementlib.api;

/**
 * 一次元素伤害的来由：反应本体 / 区域周期伤害 / 区域爆炸伤害。
 */
public enum ElementalDamageReason {

    /** 反应本体结算出来的伤害。 */
    REACTION,

    /** 区域实体每 tick 的周期伤害。 */
    AREA_TICK,

    /** 区域实体寿命到期时的爆炸伤害。 */
    AREA_EXPLODE
}
