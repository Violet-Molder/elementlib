package com.linweiyun.elementlib.core.system.combat.decay;

public class DecayGroups {
    /** 默认普通攻击组别：清除时间 50 tick，元素量序列 [1,0,0]×8（每 3 次附着 1 次）。 */
    public static final DecayGroup DEFAULT_NORMAL_ATTACK = new DecayGroup(
            50,
            DecaySequence.DEFAULT_ELEMENT
    );

    /** 默认元素战技组别：清除时间 2 tick，元素量序列全 1 长度 15（每次都可附着）。 */
    public static final DecayGroup DEFAULT_ELEMENTAL_SKILL = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15)
    );

    /** 默认元素爆发组别：清除时间 2 tick，元素量序列全 1 长度 15（每次都可附着）。 */
    public static final DecayGroup DEFAULT_ELEMENTAL_BURST = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15)
    );
}
