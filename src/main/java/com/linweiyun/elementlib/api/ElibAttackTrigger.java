package com.linweiyun.elementlib.api;

/**
 * 一次攻击是怎么发生的。
 *
 * <p>「攻击」是一次出手动作，不是「打到了实体」：对空、方块左键、实体左键都算。
 */
public enum ElibAttackTrigger {

    /** 对空出手（动作监听仍会被通知，元素没有落点）。 */
    AIR,

    /** 左键点击方块（原版挖掘的起点，每次交互一次）。 */
    BLOCK_LEFT_CLICK,

    /** 左键攻击实体。 */
    ENTITY,

    /** 动作系统的伤害点（技能 / 多段普攻的每一段）。 */
    ACTION_DAMAGE_POINT
}
