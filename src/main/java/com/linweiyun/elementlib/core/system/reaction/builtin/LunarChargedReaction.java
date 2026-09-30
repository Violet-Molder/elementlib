package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.entity.ElementalAreaEntity;
import com.linweiyun.elementlib.core.entity.ModEntities;
import com.linweiyun.elementlib.core.entity.ThunderCloudEntity;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionFeedback;
import com.linweiyun.elementlib.core.system.reaction.ReactionPriorityCalculator;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * 月感电反应 —— 水 + 雷，注册比 1:1、{@code basePriority = -1}（按默认优先级表排在普通感电之前）。
 *
 * <p>成立时在目标上方生成雷暴云，并立刻结算一次月感电伤害；雷暴云的周期与爆炸伤害由实体自己结算。
 * 变体门不放行或目标处于冻结状态时不成立。
 */
public class LunarChargedReaction extends ElementalReaction {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    /** 雷暴云生成高度：目标眼睛上方 2 格。 */
    private static final double CLOUD_HEIGHT_OFFSET = 2.0;

    public LunarChargedReaction(Supplier<ElementalReactionType> type,
                                String elementAId, String elementBId,
                                float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        if (context.targetEntity() == null) return true;
        if (ReactionPriorityCalculator.hasFrozen(context.targetContainer())) return true;
        return !ElementLibApi.variantGate().lunarCharged(context.attackerEntity(), context.targetEntity());
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement hydro = ModElements.of(ModElements.HYDRO);
        GenshinElement electro = ModElements.of(ModElements.ELECTRO);
        if (hydro == null || electro == null) {
            return ReactionResult.builder(type()).build();
        }

        ElementalAttachmentInstance hydroInst = ElectroChargedReaction.findElement(ctx.targetContainer(), hydro);
        ElementalAttachmentInstance electroInst = ElectroChargedReaction.findElement(ctx.targetContainer(), electro);
        if (hydroInst == null || electroInst == null
                || hydroInst.getUnit() <= 0 || electroInst.getUnit() <= 0) {
            return ReactionResult.builder(type()).build();
        }

        LivingEntity target = ctx.targetEntity();
        if (target == null || !(target.level() instanceof ServerLevel level)
                || ModEntities.THUNDER_CLOUD == null) {
            return ReactionResult.builder(type()).build();
        }

        float base = ElementLibConfig.baseDamage("lunar_charged");
        Vec3 cloudPos = new Vec3(target.getX(), target.getEyeY() + CLOUD_HEIGHT_OFFSET, target.getZ());
        ElementalAreaEntity.spawn(level, ModEntities.THUNDER_CLOUD.get(), cloudPos,
                ctx.attackerEntity(), ThunderCloudEntity.RADIUS, ThunderCloudEntity.DURATION_TICKS,
                base, base);

        ReactionDamage.dealDirect(ModReactionTypes.LUNAR_CHARGED.get(), electro,
                ctx.attackerEntity(), target, base);
        ReactionFeedback.reaction(target, ModReactionTypes.LUNAR_CHARGED.get(), electro);

        LOGGER.debug("[月感电] 生成雷暴云 target={} pos={}", target.getName().getString(), cloudPos);

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(ctx.attackerUnit())
                .consumedDefender(0)
                .build();
    }
}
