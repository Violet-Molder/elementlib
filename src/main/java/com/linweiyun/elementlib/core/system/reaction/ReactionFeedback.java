package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ReactionFeedbackHandler;
import com.linweiyun.elementlib.core.element.GenshinElement;
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
 */
public final class ReactionFeedback {

    /** 反馈广播半径（格）。 */
    private static final double BROADCAST_RADIUS = 12.0;

    private static final int DEFAULT_COLOR = GenshinElement.DEFAULT_FEEDBACK_COLOR;

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
        ReactionFeedbackHandler handler = ElementLibApi.feedbackHandler();
        if (handler != null) {
            handler.reaction(target, type, element);
            return;
        }
        if (target == null || type == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        spawnParticles(level, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                particleOf(element));
        sendMessage(level, target.getX(), target.getY(), target.getZ(), type);
    }
    public static void transformative(@Nullable LivingEntity target, @Nullable ElementalReactionType type,
                                      @Nullable GenshinElement element) {
        ReactionFeedbackHandler handler = ElementLibApi.feedbackHandler();
        if (handler != null) {
            handler.transformative(target, type, element);
            return;
        }
        if (target == null || type == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();

        spawnParticles(level, x, y, z, particleOf(element));
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }
    public static void atBlock(@Nullable ServerLevel level, @Nullable BlockPos pos,
                               @Nullable ElementalReactionType type) {
        ReactionFeedbackHandler handler = ElementLibApi.feedbackHandler();
        if (handler != null) {
            handler.atBlock(level, pos, type);
            return;
        }
        if (level == null || pos == null || type == null) return;

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        spawnParticles(level, x, y, z, particleOf(null));
        sendMessage(level, x, y, z, type);
    }

    public static int colorOf(@Nullable GenshinElement element) {
        if (element == null) return DEFAULT_COLOR;
        return element.getFeedbackColor();
    }

    private static ParticleOptions particleOf(@Nullable GenshinElement element) {
        ParticleOptions particle = element == null ? null : element.getFeedbackParticle();
        return particle != null ? particle : ParticleTypes.END_ROD;
    }

    private static void spawnParticles(ServerLevel level, double x, double y, double z,
                                       ParticleOptions particle) {
        level.sendParticles(particle, x, y, z, 8, 0.35, 0.35, 0.35, 0.02);
    }
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
