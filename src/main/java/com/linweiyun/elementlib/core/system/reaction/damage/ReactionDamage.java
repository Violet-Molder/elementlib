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
 *
 * <p>处理器返回 {@code <= 0} 时这一下不结算。
 */
public final class ReactionDamage {

    /** 默认伤害处理器单例。 */
    public static final ElementalDamageHandler DEFAULT = DemoDamageHandler.INSTANCE;

    private ReactionDamage() {
    }

    /**
     * 对单个目标结算一次伤害，世界从 {@code target} 取。
     *
     * @param attacker 施加伤害的一方，<b>可为 {@code null}</b>（无归属伤害，依然结算）
     * @param target   目标；为 {@code null} 时什么也不做
     */
    public static void dealDirect(ElementalReactionType type, @Nullable GenshinElement element,
                                  @Nullable Entity attacker, @Nullable LivingEntity target, float baseDamage) {
        if (target == null) {
            return;
        }
        deliver(new ElementalDamageContext(type, element, attacker, target, target.position(),
                ElementalDamageReason.REACTION, baseDamage));
    }

    /**
     * 在指定世界里对区域内的生物逐个结算：扫描 {@code pos} 半径 {@code radius} 内的
     * {@link LivingEntity}，跳过攻击者自己。
     *
     * @param attacker 施加伤害的一方，<b>可为 {@code null}</b>（无归属伤害，依然结算）
     */
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

    /**
     * 上一个重载的便捷版：世界取自 {@code attacker}；
     * {@code attacker} 为 {@code null} 或不在服务端时什么也不做（没有世界可查），
     * 需要无归属伤害请用带 {@link ServerLevel} 的重载。
     */
    public static void dealAt(ElementalReactionType type, @Nullable GenshinElement element,
                              @Nullable Entity attacker, Vec3 pos, double radius,
                              float baseDamage, ElementalDamageReason reason) {
        if (attacker == null || !(attacker.level() instanceof ServerLevel level)) {
            return;
        }
        dealAt(level, type, element, attacker, pos, radius, baseDamage, reason);
    }

    /**
     * 增幅反应倍率出口，直接转交 {@link ElementalDamageHandler#computeAmplifyMultiplier}。
     *
     * @return 实际使用的倍率
     */
    public static float amplify(ElementalReactionType type, float configured) {
        return ElementLibApi.damageHandler().computeAmplifyMultiplier(type, configured);
    }

    /** 先算后打：处理器给出的伤害 {@code <= 0} 时不结算。 */
    private static void deliver(ElementalDamageContext ctx) {
        ElementalDamageHandler handler = ElementLibApi.damageHandler();
        float amount = handler.computeDamage(ctx);
        if (amount <= 0f) {
            return;
        }
        handler.dealDamage(ctx, amount);
    }
}
