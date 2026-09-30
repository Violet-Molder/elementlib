package com.linweiyun.elementlib.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 伤害出口：反应与区域实体的伤害计算、结算、增幅倍率全部经过这里。
 *
 * <p>三个方法都有默认实现（按配置的基础值直接打原版魔法伤害），
 * 使用者只需替换关心的那一个，不必实现全部。通过
 * {@link ElementLibApi#setDamageHandler(ElementalDamageHandler)} 换掉。
 *
 * <pre>{@code
 * ElementLibApi.setDamageHandler(new ElementalDamageHandler() {
 *     @Override
 *     public float computeDamage(ElementalDamageContext ctx) {
 *         return ctx.baseDamage() * 2.0f;   // 换成自己的公式
 *     }
 * });
 * }</pre>
 */
public interface ElementalDamageHandler {

    /**
     * 计算这一次的最终伤害。
     *
     * @return 伤害值；返回 {@code <= 0} 表示这一下不结算
     */
    default float computeDamage(ElementalDamageContext ctx) {
        return ctx.baseDamage();
    }

    /**
     * 把伤害打出去。默认用 {@code level.damageSources().magic()}；
     * 目标或服务端世界缺失、{@code amount <= 0} 时什么也不做。
     */
    default void dealDamage(ElementalDamageContext ctx, float amount) {
        if (amount <= 0f || ctx == null) {
            return;
        }
        LivingEntity target = ctx.target();
        ServerLevel level = ctx.level();
        if (target == null || level == null) {
            return;
        }
        target.hurtServer(level, level.damageSources().magic(), amount);
    }

    /**
     * 增幅反应（融化 / 蒸发）的倍率出口。
     *
     * @param type       反应类型，可为 {@code null}
     * @param configured 反应自己算好的配置倍率
     * @return 实际使用的倍率
     */
    default float computeAmplifyMultiplier(ElementalReactionType type, float configured) {
        return configured;
    }
}
