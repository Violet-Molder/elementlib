package com.linweiyun.elementlib.api;

/**
 * 元素反应的分类。
 *
 * <p>分类决定反应的对外表现：{@link #LUNAR} 与 {@link #STELLAR} 不出反应飘字，
 * 它们的表现由各自的伤害链负责。
 */
public enum ReactionCategory {
    /** 增幅反应（融化 / 蒸发）。 */
    AMPLIFYING,
    /** 剧变反应（超导 / 扩散 / 感电等）。 */
    TRANSFORMATIVE,
    /** 特殊反应（冻结 / 碎冰 / 结晶）。 */
    SPECIAL,
    /** 星体系分支。 */
    STELLAR,
    /** 月体系分支。 */
    LUNAR,
    /** 其它。 */
    OTHER
}
