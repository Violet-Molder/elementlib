package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 衰减组别 —— 清除时间 + 元素量 / 伤害 / 削韧三条序列。
 *
 * <p>同标签 + 同组别 = 共用一套计时计数器。
 */
public record DecayGroup(int clearTimeTicks, DecaySequence elementSequence,
                         DecaySequence damageSequence, DecaySequence poiseSequence) {

    public static final int DEFAULT_CLEAR_TIME_TICKS = 50;

    public DecayGroup {
        java.util.Objects.requireNonNull(elementSequence, "DecayGroup.elementSequence 不能为 null");
        java.util.Objects.requireNonNull(damageSequence, "DecayGroup.damageSequence 不能为 null");
        java.util.Objects.requireNonNull(poiseSequence, "DecayGroup.poiseSequence 不能为 null");
    }

    public float getElementCoefficient(int hitCount) {
        return elementSequence.getCoefficient(hitCount);
    }

    public float getDamageCoefficient(int hitCount) {
        return damageSequence.getCoefficient(hitCount);
    }

    public float getPoiseCoefficient(int hitCount) {
        return poiseSequence.getCoefficient(hitCount);
    }
}
