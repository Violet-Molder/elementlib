package com.linweiyun.elementlib.api;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 攻击时"这个方块要不要进宿主列表"的扩展点；方块韧性用它。
 */
@FunctionalInterface
public interface ElibAttackBlockInterest {

    boolean interested(ElibAttackAction action, ServerLevel level, BlockPos pos, BlockState state);
}
