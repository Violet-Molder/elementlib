package com.linweiyun.elementlib.core.system.combat.decay;

public class DecayGroups {

    public static final DecayGroup DEFAULT_NORMAL_ATTACK = new DecayGroup(
            50,
            DecaySequence.DEFAULT_ELEMENT,
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    public static final DecayGroup DEFAULT_ELEMENTAL_SKILL = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15),
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );

    public static final DecayGroup DEFAULT_ELEMENTAL_BURST = new DecayGroup(
            2,
            DecaySequence.createFilled(1.0f, 15),
            DecaySequence.DEFAULT_DAMAGE,
            DecaySequence.DEFAULT_POISE
    );
}
