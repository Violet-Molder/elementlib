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
 *
 * <p><b>为什么存整容器而不是单条实例</b>：方块和生物一样要能同时挂多种元素
 * （水面挂水、被火打挂火…），只存一条冻的话「这个方块身上现在有什么元素」根本答不出来，
 * 规则的覆盖/损耗/反应也就无从复用。存容器后，方块与生物走同一条附着入口
 * （见 {@code core/system/about/host/BlockHost}）。
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
     *
     * <p>浮冰身上可能挂着多条方块排期（我们每 tick 重排一条 + 原版 {@code onPlace} 的随机
     * 60–120 tick 一条），多余排期会让某格在同一 tick 里多走一步衰减 —— 这就是
     * 「一起结的冰不同时化」的来源。每 tick 只推进一次即可消除，与排期条数无关。
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
