package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 反应表现出口 —— 替换后，框架不再自己发粒子/文案，全部交给使用者。
 */
public interface ReactionFeedbackHandler {

    void reaction(@Nullable LivingEntity target, @Nullable ElementalReactionType type,
                  @Nullable GenshinElement element);

    void transformative(@Nullable LivingEntity target, @Nullable ElementalReactionType type,
                        @Nullable GenshinElement element);

    void atBlock(@Nullable ServerLevel level, @Nullable BlockPos pos,
                 @Nullable ElementalReactionType type);
}
