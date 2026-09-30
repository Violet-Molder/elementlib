package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 一次元素伤害的上下文，交给 {@link ElementalDamageHandler} 作为唯一输入。
 *
 * @param reactionType 触发这次伤害的反应类型
 * @param element      参与伤害的元素，可为 {@code null}
 * @param attacker     施加伤害的一方，可为 {@code null}
 * @param target       承受伤害的生物，可为 {@code null}
 * @param position     伤害发生的位置（区域伤害为区域中心），可为 {@code null}
 * @param reason       伤害来由
 * @param baseDamage   配置给出的基础伤害值，未经计算
 */
public record ElementalDamageContext(ElementalReactionType reactionType,
                                     @Nullable GenshinElement element,
                                     @Nullable Entity attacker,
                                     @Nullable LivingEntity target,
                                     @Nullable Vec3 position,
                                     ElementalDamageReason reason,
                                     float baseDamage) {

    /**
     * 本次伤害所在的服务端世界：先取 {@link #target()}，再取 {@link #attacker()}。
     *
     * @return 服务端世界；两者都不在服务端时为 {@code null}，调用方应跳过这次结算
     */
    @Nullable
    public ServerLevel level() {
        if (target != null && target.level() instanceof ServerLevel targetLevel) {
            return targetLevel;
        }
        if (attacker != null && attacker.level() instanceof ServerLevel attackerLevel) {
            return attackerLevel;
        }
        return null;
    }
}
