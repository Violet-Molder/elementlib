package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.attachment.ElibModuleAttachments;
import com.linweiyun.elementlib.core.module.ElibModuleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 冻结方块的集中式推进 + 加载自愈。
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public final class BlockElementTicker {

    private BlockElementTicker() {
    }

    /** 每 tick 统一推进登记在案的冻结方块（内部有每格每 tick 只推进一次的去重）。 */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BlockElementHelper.trackedTick(level);
        }
    }

    /** 区块加载时把老的、还冻着的方块补进推进表。 */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getChunk() instanceof LevelChunk chunk)
                || !chunk.hasData(ElibModuleAttachments.CHUNK_MODULES.get())) {
            return;
        }
        var data = chunk.getData(ElibModuleAttachments.CHUNK_MODULES);
        for (var entry : data.getPersistent().entrySet()) {
            var container = entry.getValue();
            if (container == null || container.isEmpty()) {
                continue;
            }
            if (container.get(ElibModuleTypes.ELEMENT) == null) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.getKey());
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                BlockElementHelper.trackFrozen(level, pos);
            }
        }
    }
}
