package com.linweiyun.elementlib.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 星体系 / 月感电变体的开关门：返回 {@code true} 时对应反应走变体分支。
 */
public interface ReactionVariantGate {

    /** 星扩散：扩散元素为冰时是否走「星扩散」分支。 */
    boolean stellarSwirl(@Nullable Entity attacker, @Nullable LivingEntity target);

    /** 星超导：超导是否走「星超导」分支。 */
    boolean stellarConduce(@Nullable Entity attacker, @Nullable LivingEntity target);

    /** 月感电：感电是否走「月感电」分支。 */
    boolean lunarCharged(@Nullable Entity attacker, @Nullable LivingEntity target);
}
