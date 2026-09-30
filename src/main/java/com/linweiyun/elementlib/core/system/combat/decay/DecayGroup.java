package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 衰减组别 —— 定义攻击衰减系统的控制参数。
 *
 * <p>清除时间（{@code clearTimeTicks}）：从第一次命中开始倒计时，到期后重置计数器和定时器。
 * 元素量序列（{@code elementSequence}）：实际元素量 = 攻击原始元素量 × 序列系数，系数 0 = 不附着。
 *
 * <p>同标签 + 同组别 = 完全共用一套计时计数器；同标签 + 不同组别 = 不共用（控制参数不同）。
 */
public record DecayGroup(int clearTimeTicks, DecaySequence elementSequence) {

    // 2.5秒对应的tick数（MC中1秒=20tick），大多数攻击使用的标准清除时间
    public static final int DEFAULT_CLEAR_TIME_TICKS = 50;

    public DecayGroup {
        // 快速失败：元素量序列不能为 null
        java.util.Objects.requireNonNull(elementSequence, "DecayGroup.elementSequence 不能为 null");
    }

    // ========== Getter 方法 ==========

    @Override
    public int clearTimeTicks() {
        return clearTimeTicks;
    }

    @Override
    public DecaySequence elementSequence() {
        return elementSequence;
    }

    // ========== 便捷方法 ==========

    /**
     * 获取指定命中次数对应的元素量系数（0.0~1.0）。
     */
    public float getElementCoefficient(int hitCount) {
        return elementSequence.getCoefficient(hitCount);    // 从元素量序列获取系数
    }
}
