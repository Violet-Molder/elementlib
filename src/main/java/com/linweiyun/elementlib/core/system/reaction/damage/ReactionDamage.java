package com.linweiyun.elementlib.core.system.reaction.damage;

import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementalDamageContext;
import com.linweiyun.elementlib.api.ElementalDamageHandler;
import com.linweiyun.elementlib.api.ElementalDamageReason;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.element.GenshinElement;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 反应伤害的发放入口：组装 {@link ElementalDamageContext}，再交给
 * {@link ElementLibApi#damageHandler()} 计算与结算。
 */
public final class ReactionDamage {

    /** 默认伤害处理器单例。 */
    public static final ElementalDamageHandler DEFAULT = DemoDamageHandler.INSTANCE;

    private ReactionDamage() {
    }
    public static void dealDirect(ElementalReactionType type, @Nullable GenshinElement element,
                                  @Nullable Entity attacker, @Nullable LivingEntity target, float baseDamage) {
        if (target == null) {
            return;
        }
        deliver(new ElementalDamageContext(type, element, attacker, target, target.position(),
                ElementalDamageReason.REACTION, baseDamage));
    }
    public static void dealAt(ServerLevel level, ElementalReactionType type, @Nullable GenshinElement element,
                              @Nullable Entity attacker, Vec3 pos, double radius,
                              float baseDamage, ElementalDamageReason reason) {
        if (pos == null || radius <= 0.0) {
            return;
        }
        AABB box = new AABB(pos, pos).inflate(radius);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (victim == attacker || !victim.isAlive()) {
                continue;
            }
            if (victim.position().distanceToSqr(pos) > radius * radius) {
                continue;
            }
            deliver(new ElementalDamageContext(type, element, attacker, victim, pos, reason, baseDamage));
        }
    }
    public static void dealAt(ElementalReactionType type, @Nullable GenshinElement element,
                              @Nullable Entity attacker, Vec3 pos, double radius,
                              float baseDamage, ElementalDamageReason reason) {
        if (attacker == null || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        dealAt(level, type, element, attacker, pos, radius, baseDamage, reason);
    }

    public static float amplify(ElementalReactionType type, float configured) {
        return ElementLibApi.damageHandler().computeAmplifyMultiplier(type, configured);
    }
    private static void deliver(ElementalDamageContext ctx) {
        ElementalDamageHandler handler = ElementLibApi.damageHandler();
        float amount = handler.computeDamage(ctx);
        if (amount <= 0f) {
            return;
        }
        handler.dealDamage(ctx, amount);
    }
}
