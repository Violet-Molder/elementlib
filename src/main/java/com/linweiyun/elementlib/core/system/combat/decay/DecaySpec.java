package com.linweiyun.elementlib.core.system.combat.decay;

/**
 * 计时计数器的键描述。
 *
 * @param decayTag 衰减标签（null 表示不计数）
 * @param group    衰减组别
 */
public record DecaySpec(String decayTag, DecayGroup group) {
}
