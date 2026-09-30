package com.linweiyun.elementlib.core.system.about;

/**
 * 附着来源 —— 决定「谁和谁能相互覆盖」。
 *
 * <p>只有同元素 + 同来源的两份附着才会互相覆盖；不同来源的同元素是容器里两个独立的实例。
 * 除 {@link #NORMAL_ATTACK} 外的来源都是「直接附着」：无损耗（lossMultiplier = 1.0），
 * 且不遵循后手不残留规则。
 */
public enum AttachmentSource {

    /** 角色攻击造成的常规附着：先手 20% 损耗，触发不共存反应时遵循后手不残留。 */
    NORMAL_ATTACK,

    /** 环境附着（水渊、荆棘方块、元素方碑等）：无损耗，通常是恒定附着（被消耗后周期补充）。 */
    ENVIRONMENTAL,

    /** 武器元素附魔：无损耗，参数由天赋等级决定。 */
    WEAPON_ENCHANT,

    /** 角色自附着（如给队友挂的火）：无损耗，参数完全由天赋决定。 */
    SELF_ATTACH,

    /** 特殊附着（激/冻/燃元素、元素试炼仪等）：无损耗，有专属衰减规律，不遵循后手不残留。 */
    SPECIAL;
}
