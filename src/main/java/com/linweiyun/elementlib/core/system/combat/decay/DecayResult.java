package com.linweiyun.elementlib.core.system.combat.decay;

import lombok.Getter;

/**
 * 单次攻击的衰减系数结果。
 */
@Getter
public class DecayResult {
    public static final DecayResult NONE = new DecayResult(1.0f, 1.0f, 1.0f);

    private final float elementCoefficient;
    private final float damageCoefficient;
    private final float poiseCoefficient;

    public DecayResult(float elementCoefficient, float damageCoefficient, float poiseCoefficient) {
        this.elementCoefficient = elementCoefficient;
        this.damageCoefficient = damageCoefficient;
        this.poiseCoefficient = poiseCoefficient;
    }

    public boolean hasElementAttachment() {
        return elementCoefficient > 0;
    }

    @Override
    public String toString() {
        return "DecayResult{elem=" + elementCoefficient + ", dmg=" + damageCoefficient
                + ", poise=" + poiseCoefficient + '}';
    }
}
