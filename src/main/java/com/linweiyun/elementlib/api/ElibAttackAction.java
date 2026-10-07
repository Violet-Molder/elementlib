package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 一次攻击动作的完整描述。
 *
 * <p>与准星是否命中无关；{@link #element()} 为 {@code null} 时由注册的元素解析器决定。
 */
public record ElibAttackAction(
        Entity attacker,
        @Nullable GenshinElement element,
        AttachmentSource source,
        AttachmentProfile profile,
        ElibAttackTrigger trigger,
        String kindId,
        Vec3 origin,
        Vec3 direction,
        double reach,
        long gameTime,
        @Nullable String sourceKey,
        float poise,
        float elementAmount,
        @Nullable BlockPos targetBlock,
        boolean damageSubStep,
        @Nullable ResourceLocation originId) {

    /** 常用构造：几何取攻击者眼睛朝向，来源取主手物品。 */
    public static ElibAttackAction of(Entity attacker,
                                      @Nullable GenshinElement element,
                                      ElibAttackTrigger trigger,
                                      AttachmentSource source,
                                      AttachmentProfile profile,
                                      double reach) {
        return new ElibAttackAction(
                attacker,
                element,
                source,
                profile,
                trigger,
                trigger.name().toLowerCase(java.util.Locale.ROOT),
                attacker.getEyePosition(),
                attacker.getViewVector(1.0f),
                reach,
                attacker.level().getGameTime(),
                sourceKeyOf(attacker),
                0f,
                AttachmentProfile.WEAK.getBaseQuantity(),
                null,
                false,
                null);
    }

    /** 常用构造 + 显式几何（方块左键这种"朝着某一格出手"用它）。 */
    public static ElibAttackAction aimed(Entity attacker,
                                         @Nullable GenshinElement element,
                                         ElibAttackTrigger trigger,
                                         AttachmentSource source,
                                         AttachmentProfile profile,
                                         Vec3 origin,
                                         Vec3 direction,
                                         double reach) {
        return new ElibAttackAction(attacker, element, source, profile, trigger,
                trigger.name().toLowerCase(java.util.Locale.ROOT),
                origin, direction, reach, attacker.level().getGameTime(), sourceKeyOf(attacker),
                0f, AttachmentProfile.WEAK.getBaseQuantity(), null, false, null);
    }

    /** 左键点击方块：几何朝向目标格，并把目标格带在动作上（下游不用再射线找回）。 */
    public static ElibAttackAction forBlock(Player player, BlockPos pos,
                                            @Nullable GenshinElement element,
                                            AttachmentSource source,
                                            AttachmentProfile profile) {
        Vec3 origin = player.getEyePosition();
        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(origin);
        double distance = toBlock.length();
        Vec3 direction = distance < 1.0E-4 ? player.getViewVector(1.0f) : toBlock.normalize();
        double reach = Math.max(1.5, distance + 1.0);
        return new ElibAttackAction(player, element, source, profile,
                ElibAttackTrigger.BLOCK_LEFT_CLICK, "block_left_click",
                origin, direction, reach, player.level().getGameTime(), sourceKeyOf(player),
                0f, AttachmentProfile.WEAK.getBaseQuantity(), pos, false, null);
    }

    /** 带上一笔削韧值（方块韧性用它）。 */
    public ElibAttackAction withPoise(float value) {
        return new ElibAttackAction(attacker, element, source, profile, trigger, kindId,
                origin, direction, reach, gameTime, sourceKey, value, elementAmount, targetBlock,
                damageSubStep, originId);
    }

    /** 带上一笔附着量（0 = 这次攻击不附着）。 */
    public ElibAttackAction withElementAmount(float value) {
        return new ElibAttackAction(attacker, element, source, profile, trigger, kindId,
                origin, direction, reach, gameTime, sourceKey, poise, value, targetBlock,
                damageSubStep, originId);
    }

    /** 换一个攻击种类 id（lib 不认识的种类用字符串表达）。 */
    public ElibAttackAction withKindId(String newKindId) {
        return new ElibAttackAction(attacker, element, source, profile, trigger, newKindId,
                origin, direction, reach, gameTime, sourceKey, poise, elementAmount, targetBlock,
                damageSubStep, originId);
    }

    /** 带上来源标识，会一路传到元素附着事件。 */
    public ElibAttackAction withOriginId(@Nullable ResourceLocation newOriginId) {
        return new ElibAttackAction(attacker, element, source, profile, trigger, kindId,
                origin, direction, reach, gameTime, sourceKey, poise, elementAmount, targetBlock,
                damageSubStep, newOriginId);
    }

    /** 标记这一下是伤害管线内部的子步骤，攻击行为事件不会为它发。 */
    public ElibAttackAction asDamageSubStep() {
        return new ElibAttackAction(attacker, element, source, profile, trigger, kindId,
                origin, direction, reach, gameTime, sourceKey, poise, elementAmount, targetBlock,
                true, originId);
    }

    @Nullable
    private static String sourceKeyOf(Entity attacker) {
        if (!(attacker instanceof LivingEntity living)) {
            return null;
        }
        ItemStack stack = living.getMainHandItem();
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? null : key.toString();
    }
}
