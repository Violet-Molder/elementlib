package com.linweiyun.elementlib.core.system.combat.decay;

import lombok.Getter;

/**
 * 单次攻击的衰减系数结果，元素附着量系数 0 = 不附着、1 = 全额附着。
 */
@Getter
public class DecayResult {
    public static final DecayResult NONE = new DecayResult(1.0f);

    private final float elementCoefficient;

    public DecayResult(float elementCoefficient) {
        this.elementCoefficient = elementCoefficient;
    }

    public boolean hasElementAttachment() { return elementCoefficient > 0; }

    @Override
    public String toString() {
        return "DecayResult{elem=" + elementCoefficient + '}';
    }
}
