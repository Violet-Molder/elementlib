package com.linweiyun.elementlib.core.system.about;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 附着参数 —— 描述「这次附着本身的属性」，可随时 new 出临时参数。
 */
public class AttachmentProfile implements IPersistedSerializable {

    /** 附着量 U（损耗前的基础值，常规攻击 1/1.5/2/4 中的一个） */
    @Persisted(key = "base_quantity")
    private float baseQuantity;

    /** 附着损耗系数：常规攻击先手 0.8，直接附着 1.0（无损耗） */
    @Persisted(key = "loss_multiplier")
    private float lossMultiplier;

    /** 每秒衰减速率 U/s，恒定附着为 0 */
    @Persisted(key = "decay_per_second")
    private float decayPerSecond;

    /** 附着时间秒数，恒定附着为 -1（无限） */
    @Persisted(key = "duration_seconds")
    private float durationSeconds;

    /** 是否恒定附着（衰减 0，无限时间，被消耗后周期补充） */
    @Persisted(key = "permanent")
    private boolean permanent;

    // ========== 构造 ==========

    /** 完整构造 */
    public AttachmentProfile(float baseQuantity, float lossMultiplier,
                             float decayPerSecond, float durationSeconds) {
        this.baseQuantity = baseQuantity;
        this.lossMultiplier = lossMultiplier;
        this.decayPerSecond = decayPerSecond;
        this.durationSeconds = durationSeconds;
        this.permanent = durationSeconds < 0;
    }

    /**
     * 恒定附着专用构造（衰减 0，无限时间）。
     */
    public static AttachmentProfile permanent(float baseQuantity) {
        return new AttachmentProfile(baseQuantity, 1.0f, 0f, -1f);
    }

    /**
     * 常规附着工厂：按公式算出附着时间与衰减速率（{@code t = 7 + 2.5x}，{@code v = 0.8x / t}）。
     *
     * @param x 附着量 U：1=弱 / 1.5=中 / 2=强 / 4=超强
     */
    public static AttachmentProfile normal(float x) {
        float t = 7f + 2.5f * x;
        float v = (0.8f * x) / t;
        return new AttachmentProfile(x, 0.8f, v, t);
    }

    /**
     * 按元素量选附着档次 —— 「元素量 → 附着参数」的唯一映射。
     *
     * @param amount 本次攻击的元素量（U），{@code <= 0} 视作弱附着
     */
    public static AttachmentProfile forAmount(float amount) {
        if (amount >= 4f) return ULTRA_STRONG;
        if (amount >= 2f) return STRONG;
        if (amount >= 1.5f) return MEDIUM;
        return WEAK;
    }

    // ========== 五个预设常量 ==========

    /** 弱附着：1U → 损耗后 0.8U，衰减 ≈ 0.084U/s，时长 9.5s */
    public static final AttachmentProfile WEAK = normal(1.0f);

    /** 中附着：1.5U → 损耗后 1.2U，时长 10.75s */
    public static final AttachmentProfile MEDIUM = normal(1.5f);

    /** 强附着：2U → 损耗后 1.6U，时长 12s */
    public static final AttachmentProfile STRONG = normal(2.0f);

    /** 超强附着：4U → 损耗后 3.2U，时长 17s */
    public static final AttachmentProfile ULTRA_STRONG = normal(4.0f);

    /** 恒定附着：无限时间，衰减 0。荆棘方块、元素方碑等用 */
    public static final AttachmentProfile PERMANENT = permanent(4.0f);

    // ========== 计算 ==========

    /** 实际附着量 = baseQuantity × lossMultiplier */
    public float actualQuantity() {
        return baseQuantity * lossMultiplier;
    }

    // ========== Getter ==========

    public float getBaseQuantity() { return baseQuantity; }
    public float getLossMultiplier() { return lossMultiplier; }
    public float getDecayPerSecond() { return decayPerSecond; }
    public float getDurationSeconds() { return durationSeconds; }
    public boolean isPermanent() { return permanent; }
}
