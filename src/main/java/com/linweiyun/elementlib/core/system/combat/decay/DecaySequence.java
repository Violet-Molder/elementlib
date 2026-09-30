package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 衰减序列 —— 攻击衰减系统中的序列数据。
 */
public class DecaySequence {

    // 每个位置对应一次攻击命中时的调整系数：1.0 不衰减，0.0 完全无效，0.5 减半
    private final float[] coefficients;
    public DecaySequence(float[] coefficients) {
        this.coefficients = coefficients;
    }

    /**
     * 获取指定命中次数对应的序列系数，超出序列长度返回 0。
     *
     * @param hitCount 攻击命中次数（从 0 开始，第 1 次命中 = 0）
     */
    public float getCoefficient(int hitCount) {
        if (hitCount < 0 || hitCount >= coefficients.length) return 0.0f;
        return coefficients[hitCount];
    }
    public int length() {
        return coefficients.length;
    }
    public float[] getCoefficients() {
        return coefficients;
    }

    public static final DecaySequence DEFAULT_ELEMENT = createRepeating(
            new float[]{1.0f, 0.0f, 0.0f}, 8);

    /**
     * 将基础模式重复指定次数，生成最终序列。
     *
     * @param pattern 基础模式数组
     * @param repeatCount 重复次数
     */
    public static DecaySequence createRepeating(float[] pattern, int repeatCount) {
        float[] result = new float[pattern.length * repeatCount];   // 创建结果数组
        for (int i = 0; i < repeatCount; i++) {                     // 遍历重复次数
            System.arraycopy(pattern, 0, result, i * pattern.length, pattern.length); // 复制模式
        }
        return new DecaySequence(result);                           // 返回新序列
    }
    public static DecaySequence createFilled(float value, int length) {
        float[] result = new float[length];                         // 创建结果数组
        java.util.Arrays.fill(result, value);                       // 填充所有位置
        return new DecaySequence(result);                           // 返回新序列
    }

    public static DecaySequence of(float... coefficients) {
        return new DecaySequence(coefficients);                     // 直接创建
    }
}
