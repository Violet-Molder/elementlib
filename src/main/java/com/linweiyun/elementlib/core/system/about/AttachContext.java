package com.linweiyun.elementlib.core.system.about;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * 一次附着的来源上下文 —— 附着入口内部触发反应时要用的「谁挂的、算多少」。
 *
 * @param sourceKey       挂上这条附着的来源标识（用于同源实例匹配，可空）
 * @param gameTime        附着时刻（可空：传 0 表示不记录）
 * @param attackerEntity  攻击者实体（反应要用它算来源，可空）
 * @param reactionUnit    触发反应时使用的元素量；{@code null} 表示用实际附着量
 * @param originId        来源标识：具体是哪一个环境、哪一招（可空）。
 *                        与 {@link AttachmentSource} 分工不同 —— 后者决定覆盖规则，本字段只回答「这次是谁挂的」。
 */
public record AttachContext(@Nullable String sourceKey,
                            long gameTime,
                            @Nullable Entity attackerEntity,
                            @Nullable Float reactionUnit,
                            @Nullable Identifier originId) {

    /** 兼容构造：不带来源标识。 */
    public AttachContext(@Nullable String sourceKey, long gameTime,
                         @Nullable Entity attackerEntity, @Nullable Float reactionUnit) {
        this(sourceKey, gameTime, attackerEntity, reactionUnit, null);
    }

    /** 环境/自身附着：没有来源、没有攻击者。 */
    public static final AttachContext ENVIRONMENT = new AttachContext(null, 0L, null, null, null);

    /** 攻击型附着：带来源标识、时刻、攻击者，并按给定元素量触发反应。 */
    public static AttachContext attack(@Nullable String sourceKey, long gameTime,
                                       @Nullable Entity attackerEntity, float reactionUnit) {
        return new AttachContext(sourceKey, gameTime, attackerEntity, reactionUnit, null);
    }

    /** 攻击型附着 + 来源标识。 */
    public static AttachContext attack(@Nullable String sourceKey, long gameTime,
                                       @Nullable Entity attackerEntity, float reactionUnit,
                                       @Nullable Identifier originId) {
        return new AttachContext(sourceKey, gameTime, attackerEntity, reactionUnit, originId);
    }

    /** 反应内的二次写入：带攻击者用于后续表现，但不覆盖元素量。 */
    public static AttachContext reactionWrite(@Nullable Entity attackerEntity) {
        return new AttachContext(null, 0L, attackerEntity, null, null);
    }

    /** 反应内的二次写入 + 来源标识（反应 id，例如 {@code elementlib:reaction/freeze}）。 */
    public static AttachContext reactionWrite(@Nullable Entity attackerEntity,
                                              @Nullable Identifier originId) {
        return new AttachContext(null, 0L, attackerEntity, null, originId);
    }

    /** 换一个来源标识，其余字段不变。 */
    public AttachContext withOriginId(@Nullable Identifier newOriginId) {
        return new AttachContext(sourceKey, gameTime, attackerEntity, reactionUnit, newOriginId);
    }
}