package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * 方块元素容器的存取层 —— 「方块宿主的状态挂在 Chunk 数据上」这一件事只在这里出现。
 *
 * <p>写回时机与实体侧一致：改完容器对象后调 {@link #commit}（相当于实体的 {@code setData}）。
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
     *
     * <p>浮冰身上可能同时挂着多条方块排期（我们自己每 tick 重排一条、原版
     * {@code FrostedIceBlock.onPlace} 还会排一条随机 60–120 tick 的），那些多余的排期
     * 会让某一格在同一个 tick 里多走一步衰减 —— 表现就是「一起结的冰不同时化」。
     * 用「每 tick 只允许推进一次」把重复推进挡掉，跟排期有几条无关。
     *
     * @return true 表示本 tick 第一次推进（调用方继续）；false 表示本 tick 已经推进过（跳过）
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
     *
     * <p>写回是<b>幂等</b>的：容器已经在这张表里（同一个对象引用）时直接返回 ——
     * 一次多段攻击会对同一格方块反复附着，命中的又是同一份容器，
     * 每次 {@code setData} 都只是把整张表再标脏一遍，没有意义。
     *
     * <p>第一次把某个坐标的容器放进表里时必然要写回（那时 {@code peek} 还是 {@code null}），
     * 所以「区块被标脏」这件事不会漏：真正的状态变更只有第一次。
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
