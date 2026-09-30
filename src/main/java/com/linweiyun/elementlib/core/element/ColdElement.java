package com.linweiyun.elementlib.core.element;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * <b>寒元素</b> —— 冰/冻的附加效果载体。
 *
 * @see com.linweiyun.elementlib.core.system.about.ColdAura
 */
public class ColdElement extends GenshinElement {

    /** 减速修饰符的 id（随实体存进存档，不可改名）。 */
    private static final Identifier SLOW_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("elementlib", "cryo_slow");

    /** 减速幅度（-10% 移速）。 */
    private static final float SLOW_AMOUNT = -0.10f;

    protected ColdElement(String translationKey) {
        // 效果载体：不参与反应配对，也不在 HUD 上占一个图标
        super(false, false, true, translationKey);
    }

    /**
     * 按「容器里的三个事实」应用或撤销效果。
     *
     * @param cold       此刻宿主身上有没有寒（被宿主拒收时永远为 false → 天然豁免）
     * @param cryoFamily 有没有冰或冻（有冻也算有冰族，冻结期间不该因为冰被吃光而丢减速判定）
     * @param frozen     有没有冻
     */
    public static void applyEffects(LivingEntity entity, boolean cold,
                                    boolean cryoFamily, boolean frozen) {
        if (entity == null || !isNonPlayerLiving(entity)) {
            return;
        }
        applyChill(entity, cold && cryoFamily);
        applyFreeze(entity, cold && frozen);
    }

    /** 寒冷：减速。判到「状态没变」就不动属性，避免每 tick 反复加减修饰符。 */
    private static void applyChill(LivingEntity entity, boolean chilled) {
        AttributeInstance speed = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean has = speed.hasModifier(SLOW_MODIFIER_ID);
        if (chilled && !has) {
            speed.addPermanentModifier(new AttributeModifier(
                    SLOW_MODIFIER_ID, SLOW_AMOUNT,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!chilled && has) {
            speed.removeModifier(SLOW_MODIFIER_ID);
        }
    }

    /**
     * 冻结：禁 AI —— 直接改 {@link Mob#setNoAi(boolean)}（只对生物实体生效）。
     */
    private static void applyFreeze(LivingEntity entity, boolean frozen) {
        if (entity instanceof Mob mob) {
            mob.setNoAi(frozen);
        }
    }
}
