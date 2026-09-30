package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 衰减组别 —— 定义攻击衰减系统的控制参数。
 */
public record DecayGroup(int clearTimeTicks, DecaySequence elementSequence) {
    public static final int DEFAULT_CLEAR_TIME_TICKS = 50;

    public DecayGroup {
        java.util.Objects.requireNonNull(elementSequence, "DecayGroup.elementSequence 不能为 null");
    }

    @Override
    public int clearTimeTicks() {
        return clearTimeTicks;
    }

    @Override
    public DecaySequence elementSequence() {
        return elementSequence;
    }

    public float getElementCoefficient(int hitCount) {
        return elementSequence.getCoefficient(hitCount);
    }
}
