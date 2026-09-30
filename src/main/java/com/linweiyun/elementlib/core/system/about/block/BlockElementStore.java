package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * 方块元素容器的存取层 —— 「方块宿主的状态挂在 Chunk 数据上」这一件事只在这里出现。
 */
public final class BlockElementStore {

    private BlockElementStore() {
    }

    private static ChunkBlockElements data(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        return chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
    }

    /**
     * 取某个方块的元素容器（可写）。没有就现建一个并落盘 —— 这样调用方拿到的容器
     * 一直有对象可用，不必到处判空。
     */
    public static StatusContainer container(ServerLevel level, BlockPos pos) {
        return data(level, pos).get(pos);
    }

    /** 只读查询：这个方块身上有没有挂过元素。 */
    @Nullable
    public static StatusContainer peek(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS)) {
            return null;
        }
        return chunk.getData(ElementalAttachments.CHUNK_ELEMENTS).peek(pos);
    }

    /** 记下这格水冻之前的水位（level），化回来时还原 */
    public static void putWaterLevel(ServerLevel level, BlockPos pos, int waterLevel) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        elements.putWaterLevel(pos, waterLevel);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS, elements);
    }

    /** 取出并清掉记录的水位；没有记录返回 null（那时按完整水源处理） */
    public static Integer takeWaterLevel(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS)) {
            return null;
        }
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        Integer lv = elements.removeWaterLevel(pos);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS, elements);
        return lv;
    }
    /**
     * 这一格在本 game tick 是否<b>还没</b>推进过衰减。
     */
    public static boolean beginDecayStep(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        long now = level.getGameTime();
        Long last = elements.getLastDecayTick(pos);
        if (last != null && last == now) {
            return false;
        }
        elements.putLastDecayTick(pos, now);
        // 注意：这里**不** setData —— 它只是派生状态，每 tick 落盘/同步是掉帧的元凶。
        return true;
    }

    /**
     * 把容器改动写回 Chunk 数据（标记存档）。
     */
    public static void commit(ServerLevel level, BlockPos pos, StatusContainer container) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        if (elements.peek(pos) == container) {
            return;
        }
        elements.put(pos, container);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS, elements);
    }

    /** 彻底移除这个方块的元素容器（方块变成水/空气等不再承载元素的形态时调用）。 */
    public static void clear(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS)) {
            return;
        }
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        elements.remove(pos);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS, elements);
    }
}
