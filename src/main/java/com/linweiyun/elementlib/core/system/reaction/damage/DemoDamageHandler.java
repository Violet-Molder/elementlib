package com.linweiyun.elementlib.core.system.reaction.damage;

import com.linweiyun.elementlib.api.ElementalDamageContext;
import com.linweiyun.elementlib.api.ElementalDamageHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 默认伤害处理器：按配置的基础值直接结算，用原版魔法伤害源打出去。
 */
public final class DemoDamageHandler implements ElementalDamageHandler {
    public static final DemoDamageHandler INSTANCE = new DemoDamageHandler();

    private DemoDamageHandler() {
    }

    @Override
    public void dealDamage(ElementalDamageContext ctx, float amount) {
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

    @Override
    public String toString() {
        return "DemoDamageHandler";
    }
}
