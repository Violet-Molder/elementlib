package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 衰减序列 —— 实际值 = 原始值 × 序列[命中次数]，超出长度返回 0。
 */
public class DecaySequence {

    private final float[] coefficients;
    private final boolean alwaysOne;

    public DecaySequence(float[] coefficients) {
        this(coefficients, false);
    }

    private DecaySequence(float[] coefficients, boolean alwaysOne) {
        this.coefficients = coefficients;
        this.alwaysOne = alwaysOne;
    }

    public float getCoefficient(int hitCount) {
        if (alwaysOne) return 1.0f;
        if (hitCount < 0 || hitCount >= coefficients.length) return 0.0f;
        return coefficients[hitCount];
    }

    public boolean isAlwaysOne() {
        return alwaysOne;
    }

    public int length() {
        return coefficients.length;
    }

    public float[] getCoefficients() {
        return coefficients;
    }

    /** 默认元素量序列：[1,0,0] 重复 8 次（长度 24）。 */
    public static final DecaySequence DEFAULT_ELEMENT = createRepeating(
            new float[]{1.0f, 0.0f, 0.0f}, 8);

    /** 不衰减单例；必须声明在引用它的常量之前。 */
    private static final DecaySequence ALWAYS_ONE = new DecaySequence(new float[0], true);

    public static DecaySequence noDecay() {
        return ALWAYS_ONE;
    }

    /** 默认伤害序列：不衰减。 */
    public static final DecaySequence DEFAULT_DAMAGE = noDecay();

    /** 默认削韧序列：不衰减。 */
    public static final DecaySequence DEFAULT_POISE = noDecay();

    public static DecaySequence createRepeating(float[] pattern, int repeatCount) {
        float[] result = new float[pattern.length * repeatCount];
        for (int i = 0; i < repeatCount; i++) {
            System.arraycopy(pattern, 0, result, i * pattern.length, pattern.length);
        }
        return new DecaySequence(result);
    }

    public static DecaySequence createFilled(float value, int length) {
        float[] result = new float[length];
        java.util.Arrays.fill(result, value);
        return new DecaySequence(result);
    }

    public static DecaySequence of(float... coefficients) {
        return new DecaySequence(coefficients);
    }
}
