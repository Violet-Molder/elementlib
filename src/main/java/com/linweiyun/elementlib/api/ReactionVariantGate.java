package com.linweiyun.elementlib.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 星体系 / 月感电变体的开关门：返回 {@code true} 时对应反应走变体分支。
 *
 * <p>默认实现 {@code VariantGateHolder.DemoVariantGate} 读 {@link DemoContentToggles}，
 * 由命令或物品切换；接入真实角色判定时用
 * {@link ElementLibApi#setVariantGate(ReactionVariantGate)} 替换。
 */
public interface ReactionVariantGate {

    /** 星扩散：扩散元素为冰时是否走「星扩散」分支。 */
    boolean stellarSwirl(@Nullable Entity attacker, @Nullable LivingEntity target);

    /** 星超导：超导是否走「星超导」分支。 */
    boolean stellarConduce(@Nullable Entity attacker, @Nullable LivingEntity target);

    /** 月感电：感电是否走「月感电」分支。 */
    boolean lunarCharged(@Nullable Entity attacker, @Nullable LivingEntity target);
}
