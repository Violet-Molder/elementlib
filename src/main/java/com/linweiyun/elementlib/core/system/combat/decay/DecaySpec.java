package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 计时计数器的键描述。
 *
 * <p>计数器只关心两件事：衰减标签（决定是否共用计数器）与衰减组别（决定控制幅度）。
 *
 * @param decayTag 衰减标签（null 表示不计数）
 * @param group    衰减组别
 */
public record DecaySpec(String decayTag, DecayGroup group) {
}
