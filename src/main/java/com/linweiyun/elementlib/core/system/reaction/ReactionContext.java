package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 反应执行上下文 —— 携带一次元素反应需要的全部环境数据
 *
 * @param attackerElement  后手附着的元素（触发反应的那一个）
 * @param attackerUnit 后手附着的元素量
 * @param attackerSource   后手附着的来源
 * @param attackerProfile  后手附着的附着参数（用于残留判断）
 * @param attackerEntity   伤害源实体（攻击者）
 * @param targetContainer  目标身上的状态容器（反应要消耗里面的元素）
 * @param targetEntity     目标实体（用于元素附着/移除时的效果钩子）
 * @param targetHost       目标宿主（用于第二段筛查：这个宿主收不收这个反应；没有宿主信息时为 null）
 * @param originId         后手附着的来源标识（什么环境、哪一招；可空）
 */
public record ReactionContext(GenshinElement attackerElement, float attackerUnit,
                              AttachmentSource attackerSource, AttachmentProfile attackerProfile,
                              @Nullable String sourceKey,
                              Entity attackerEntity,
                              StatusContainer targetContainer, LivingEntity targetEntity,
                              ElementalHost targetHost,
                              @Nullable Identifier originId) {

    /** 兼容构造：不带来源标识。 */
    public ReactionContext(GenshinElement attackerElement, float attackerUnit,
                           AttachmentSource attackerSource, AttachmentProfile attackerProfile,
                           @Nullable String sourceKey,
                           Entity attackerEntity,
                           StatusContainer targetContainer, LivingEntity targetEntity,
                           ElementalHost targetHost) {
        this(attackerElement, attackerUnit, attackerSource, attackerProfile, sourceKey,
                attackerEntity, targetContainer, targetEntity, targetHost, null);
    }

    public boolean attackerFollowsNoResidualRule() {
        return attackerSource == AttachmentSource.NORMAL_ATTACK;
    }
}
