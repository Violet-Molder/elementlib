package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
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
 *
 * <p>推进不靠原版方块 tick 链：那格一旦被排期/邻居变化/区块边界搅断就再也没人推它，
 * 表现是「一片冰里边缘逐个化、中间一直冻着」。这里每 tick 统一推动且带上限。
 *
 * <p>推进表是内存态，区块加载时扫一遍元素容器把「是浮冰、且这格有元素数据」的位置补进表。
 * 判据里不能要求「有活着的冻元素」—— 老冰的冻元素可能早就耗尽了，要求它就会永远认不出来。
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
                || !chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS)) {
            return;
        }
        var data = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        for (var entry : data.getContainers().entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            BlockPos pos = BlockPos.of(entry.getKey());
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                BlockElementHelper.trackFrozen(level, pos);
            }
        }
    }
}
