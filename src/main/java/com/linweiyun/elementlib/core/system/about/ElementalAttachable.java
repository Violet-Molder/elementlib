package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementalReactionType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生物侧的元素规则接口 —— 由 mixin 注入到 {@code LivingEntity}，怪物可覆盖。
 */
public interface ElementalAttachable {
    boolean onAttachElement(GenshinElement element, AttachmentSource source, AttachmentProfile profile);
    default boolean onReactElement(GenshinElement attackerElement,
                                   GenshinElement defenderElement,
                                   ElementalReactionType reactionType) {
        return true;
    }
    default boolean isImmuneToElementDamage(GenshinElement element) {
        return false;
    }

    static boolean isImmuneToDamage(LivingEntity entity, GenshinElement element) {
        return entity instanceof ElementalAttachable attachable
                && attachable.isImmuneToElementDamage(element);
    }
}
