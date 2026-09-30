package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 反应反馈 —— 所有「反应表现」的唯一出口：按元素发粒子，给附近玩家发 action bar 文案，
 * 剧变反应额外播一个原版音效。
 *
 * <p>纯服务端：调用方在客户端跑也直接返回，不引用任何客户端类。
 */
public final class ReactionFeedback {

    /** 反馈广播半径（格）。 */
    private static final double BROADCAST_RADIUS = 12.0;

    private static final int DEFAULT_COLOR = 0xFFFFFF;

    private ReactionFeedback() {
    }

    // ==================== 对外入口 ====================

    /**
     * 元素反应反馈：粒子 + 附近玩家 action bar 文案。
     *
     * @param target  反馈落点；为 null 时直接返回
     * @param type    反应类型，决定文案；为 null 时直接返回
     * @param element 参与反应的元素，决定粒子与颜色；可为 null，退化为默认
     */
    public static void reaction(@Nullable LivingEntity target, @Nullable ElementalReactionType type,
                                @Nullable GenshinElement element) {
        if (target == null || type == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        spawnParticles(level, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                particleOf(element));
        sendMessage(level, target.getX(), target.getY(), target.getZ(), type);
    }

    /**
     * 剧变反应（超导 / 感电 / 扩散）反馈：粒子 + 一个原版音效。
     *
     * <p>文案由 {@code ElementalReactionManager} 统一出，这里只做视觉与听觉表现，避免重复。
     */
    public static void transformative(@Nullable LivingEntity target, @Nullable ElementalReactionType type,
                                      @Nullable GenshinElement element) {
        if (target == null || type == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        spawnParticles(level, x, y, z, particleOf(element));
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** 方块上的反应反馈：以方块中心为锚点。 */
    public static void atBlock(@Nullable ServerLevel level, @Nullable BlockPos pos,
                               @Nullable ElementalReactionType type) {
        if (level == null || pos == null || type == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        spawnParticles(level, x, y, z, particleOf(null));
        sendMessage(level, x, y, z, type);
    }

    /** 元素 → 0xRRGGBB 颜色。未知 / 空元素返回白色。 */
    public static int colorOf(@Nullable GenshinElement element) {
        if (element == null) return DEFAULT_COLOR;
        if (ModElements.is(element, ModElements.PYRO)) return 0xFF7043;
        if (ModElements.is(element, ModElements.HYDRO)) return 0x4FC3F7;
        if (ModElements.is(element, ModElements.ELECTRO)) return 0xB388FF;
        if (ModElements.is(element, ModElements.CYRO)
                || ModElements.is(element, ModElements.FROZEN)
                || ModElements.is(element, ModElements.COLD)) return 0x99FFFF;
        if (ModElements.is(element, ModElements.ANEMO)) return 0x74C2A8;
        if (ModElements.is(element, ModElements.DENDRO)) return 0x9CCC65;
        if (ModElements.is(element, ModElements.GEO)) return 0xFFD54F;
        return DEFAULT_COLOR;
    }

    // ==================== 内部 ====================

    /** 元素 → 粒子类型（与 {@link #colorOf} 同一套元素分组）。 */
    private static ParticleOptions particleOf(@Nullable GenshinElement element) {
        if (element == null) return ParticleTypes.END_ROD;
        if (ModElements.is(element, ModElements.FYSIKOS) || ModElements.is(element, ModElements.GEO)) {
            return ParticleTypes.CRIT;
        }
        if (ModElements.is(element, ModElements.PYRO)) return ParticleTypes.FLAME;
        if (ModElements.is(element, ModElements.HYDRO)) return ParticleTypes.SPLASH;
        if (ModElements.is(element, ModElements.ELECTRO)) return ParticleTypes.ELECTRIC_SPARK;
        if (ModElements.is(element, ModElements.CYRO)
                || ModElements.is(element, ModElements.FROZEN)
                || ModElements.is(element, ModElements.COLD)) return ParticleTypes.SNOWFLAKE;
        if (ModElements.is(element, ModElements.ANEMO)) return ParticleTypes.CLOUD;
        if (ModElements.is(element, ModElements.DENDRO)) return ParticleTypes.HAPPY_VILLAGER;
        return ParticleTypes.END_ROD;
    }

    private static void spawnParticles(ServerLevel level, double x, double y, double z,
                                       ParticleOptions particle) {
        level.sendParticles(particle, x, y, z, 8, 0.35, 0.35, 0.35, 0.02);
    }

    /** 给 (x,y,z) 半径 {@value #BROADCAST_RADIUS} 格内的玩家发 action bar 文案。 */
    private static void sendMessage(ServerLevel level, double x, double y, double z,
                                    ElementalReactionType type) {
        Component message = Component.translatable(type.getTranslationKey());
        double rSq = BROADCAST_RADIUS * BROADCAST_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > rSq) continue;
            player.sendOverlayMessage(message);
        }
    }
}
