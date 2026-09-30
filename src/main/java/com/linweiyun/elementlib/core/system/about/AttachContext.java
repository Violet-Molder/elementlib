package com.linweiyun.elementlib.core.system.about;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 一次附着的来源上下文 —— 附着入口内部触发反应时要用的「谁挂的、算多少」。
 *
 * <p>大多数来源只需要默认值；攻击管线会带上攻击者实体与一个元素量覆盖值：
 * 衰减系数削减的是这次附着的「反应用量」，附着本身仍按 profile 算。
 *
 * @param sourceKey       挂上这条附着的来源标识（用于同源实例匹配，可空）
 * @param gameTime        附着时刻（可空：传 0 表示不记录）
 * @param attackerEntity  攻击者实体（反应要用它算来源，可空）
 * @param reactionUnit    触发反应时使用的元素量；{@code null} 表示用实际附着量
 */
public record AttachContext(@Nullable String sourceKey,
                           long gameTime,
                           @Nullable Entity attackerEntity,
                           @Nullable Float reactionUnit) {

    /** 环境/自身附着：没有来源、没有攻击者。 */
    public static final AttachContext ENVIRONMENT = new AttachContext(null, 0L, null, null);

    /** 攻击型附着：带来源标识、时刻、攻击者，并按给定元素量触发反应。 */
    public static AttachContext attack(@Nullable String sourceKey, long gameTime,
                                       @Nullable Entity attackerEntity,
                                       float reactionUnit) {
        return new AttachContext(sourceKey, gameTime, attackerEntity, reactionUnit);
    }

    /** 反应内的二次写入：带攻击者用于后续表现，但不覆盖元素量。 */
    public static AttachContext reactionWrite(@Nullable Entity attackerEntity) {
        return new AttachContext(null, 0L, attackerEntity, null);
    }
}
