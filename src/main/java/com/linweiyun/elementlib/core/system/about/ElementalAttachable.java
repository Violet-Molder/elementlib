package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生物侧的元素规则接口 —— 由 mixin 注入到 {@code LivingEntity}，怪物可覆盖。
 *
 * <p>{@link com.linweiyun.elementlib.core.system.about.host.EntityHost} 把两段筛查委托到这里，
 * 方块侧则由 {@code BlockHost} 实现，两边最终汇进同一条附着入口。本接口回答三个互相独立的问题：
 * 这次附着收不收（{@link #onAttachElement}）、这个反应能不能发生（{@link #onReactElement}）、
 * 这个元素伤害吃不吃（{@link #isImmuneToElementDamage}）。默认实现恒 {@code true} / {@code false}。
 */
public interface ElementalAttachable {

    /**
     * 第一段筛查：这次附着收不收。
     * @return true 允许附着，false 拒绝
     */
    boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);

    /**
     * 第二段筛查：某个反应能不能发生在这个生物身上。
     *
     * <p>用于表达「允许挂水、但不接受冻结」这类规则：返回 {@code false} 时该反应被跳过，
     * 先手元素保留（共存），不会生成冻元素。
     */
    default boolean onReactElement(GenshinElement attackerElement,
                                   GenshinElement defenderElement,
                                   ElementalReactionType reactionType) {
        return true;
    }

    /**
     * <b>伤害侧</b>（不是筛查）：吃不吃这个元素造成的伤害。
     *
     * <p>它和「收不收附着」是两件事：免疫的单位照样被挂上元素、照样参与反应，只是这一下伤害按 0 结算。
     */
    default boolean isImmuneToElementDamage(GenshinElement element) {
        return false;
    }

    /** 便捷判定：这个实体（可能没有实现本接口）免疫这个元素的伤害吗。 */
    static boolean isImmuneToDamage(LivingEntity entity, GenshinElement element) {
        return entity instanceof ElementalAttachable attachable
                && attachable.isImmuneToElementDamage(element);
    }
}
