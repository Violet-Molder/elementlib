package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Chunk 级方块元素容器表 —— 每个坐标一个完整的 {@link StatusContainer}。
 */
public class ChunkBlockElements implements IPersistedSerializable {

    public static final Codec<ChunkBlockElements> CODEC =
            PersistedParser.createCodec(ChunkBlockElements::new);
    public static final StreamCodec<ByteBuf, ChunkBlockElements> STREAM_CODEC =
            PersistedParser.createStreamCodec(ChunkBlockElements::new);

    /** 冻之前那格水的水位（level），用于化回来时还原成「原来的水」而不是完整水方块。 */
    @Persisted(key = "water_levels")
    private Map<Long, Integer> waterLevels = new HashMap<>();

    public Integer getWaterLevel(BlockPos pos) {
        return waterLevels.get(pos.asLong());
    }

    public void putWaterLevel(BlockPos pos, int level) {
        waterLevels.put(pos.asLong(), level);
    }

    public Integer removeWaterLevel(BlockPos pos) {
        return waterLevels.remove(pos.asLong());
    }





    /**
     * 上次推进衰减的 gameTime。
     */
    private final Map<Long, Long> lastDecayTicks = new HashMap<>();

    public Long getLastDecayTick(BlockPos pos) { return lastDecayTicks.get(pos.asLong()); }

    public void putLastDecayTick(BlockPos pos, long tick) { lastDecayTicks.put(pos.asLong(), tick); }

    /** 坐标（{@link BlockPos#asLong()}）→ 该方块的元素容器。 */
    @Persisted(key = "containers")
    private Map<Long, StatusContainer> containers = new HashMap<>();

    public ChunkBlockElements() {}

    public boolean isEmpty() {
        return containers.isEmpty();
    }

    public Map<Long, StatusContainer> getContainers() {
        return containers;
    }

    /** 取某个方块的容器；没有就现建一个（不落盘，调用方改完要 commit）。 */
    public StatusContainer get(BlockPos pos) {
        return containers.computeIfAbsent(pos.asLong(), k -> new StatusContainer());
    }

    /** 取某个方块的容器；没有则返回 {@code null}（只读场景用）。 */
    public StatusContainer peek(BlockPos pos) {
        return containers.get(pos.asLong());
    }

    public void put(BlockPos pos, StatusContainer container) {
        containers.put(pos.asLong(), container);
    }

    public void remove(BlockPos pos) {
        containers.remove(pos.asLong());
        waterLevels.remove(pos.asLong());
    }

    /** 只清容器、保留水位（模块迁移用）。 */
    public void removeContainerOnly(BlockPos pos) {
        containers.remove(pos.asLong());
    }

    /** 清掉所有已经空掉的容器，返回清掉的个数。 */
    public int pruneEmpty() {
        int removed = 0;
        Iterator<Map.Entry<Long, StatusContainer>> it = containers.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isEmpty()) {
                it.remove();
                removed++;
            }
        }
        return removed;
    }
}
