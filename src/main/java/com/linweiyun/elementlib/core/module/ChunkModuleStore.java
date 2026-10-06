package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.ElibModuleAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.system.about.block.ChunkBlockElements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 方块模块容器的存取层；同时负责把旧 {@code chunk_elements} 的元素数据迁进来。
 */
public final class ChunkModuleStore {

    private ChunkModuleStore() {
    }

    public static ChunkModules data(LevelChunk chunk) {
        return chunk.getData(ElibModuleAttachments.CHUNK_MODULES);
    }

    /** 可写容器：没有就现建，并把旧存档的元素容器迁移过来。 */
    public static ElibModuleContainer container(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkModules modules = data(chunk);
        long key = pos.asLong();
        ElibModuleContainer container = modules.getPersistent(key);
        if (container == null) {
            container = new ElibModuleContainer();
            modules.putPersistent(key, container);
            chunk.setData(ElibModuleAttachments.CHUNK_MODULES.get(), modules);
        }
        if (!container.has(ElibModuleTypes.ELEMENT)) {
            migrateLegacy(level, pos, container);
        }
        return container;
    }

    @Nullable
    public static ElibModuleContainer peek(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElibModuleAttachments.CHUNK_MODULES.get())) {
            return null;
        }
        return data(chunk).getPersistent(pos.asLong());
    }

    public static StatusContainer element(ServerLevel level, BlockPos pos) {
        return container(level, pos).ensure(ElibModuleTypes.ELEMENT);
    }

    @Nullable
    public static StatusContainer peekElement(ServerLevel level, BlockPos pos) {
        ElibModuleContainer container = peek(level, pos);
        return container == null ? null : container.get(ElibModuleTypes.ELEMENT);
    }

    /** 瞬态容器：不写存档，区块重载后从初始值开始（方块韧性用它）。 */
    public static ElibModuleContainer transientContainer(ServerLevel level, BlockPos pos) {
        return data(level.getChunkAt(pos)).transientAt(pos.asLong());
    }

    @Nullable
    public static ElibModuleContainer peekTransient(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElibModuleAttachments.CHUNK_MODULES.get())) {
            return null;
        }
        return data(chunk).getTransient(pos.asLong());
    }

    public static void commit(ServerLevel level, BlockPos pos, ElibModuleContainer container) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkModules modules = data(chunk);
        modules.putPersistent(pos.asLong(), container);
        chunk.setData(ElibModuleAttachments.CHUNK_MODULES.get(), modules);
    }

    public static boolean removeModule(ServerLevel level, BlockPos pos, ElibModuleType<?> type) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElibModuleAttachments.CHUNK_MODULES.get())) {
            return false;
        }
        ChunkModules modules = data(chunk);
        ElibModuleContainer container = modules.getPersistent(pos.asLong());
        if (container == null || !container.remove(type)) {
            return false;
        }
        if (container.isEmpty()) {
            modules.removePersistent(pos.asLong());
        }
        chunk.setData(ElibModuleAttachments.CHUNK_MODULES.get(), modules);
        return true;
    }

    /** 清掉这一格的元素数据（元素容器 + 水位 + 衰减去重标记）。 */
    public static void clearElement(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        removeModule(level, pos, ElibModuleTypes.ELEMENT);
        ChunkModules modules = data(chunk);
        modules.removeLastDecayTick(pos.asLong());
        if (chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS.get())) {
            ChunkBlockElements legacy = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
            legacy.remove(pos);
            chunk.setData(ElementalAttachments.CHUNK_ELEMENTS.get(), legacy);
        }
    }

    /** 每 tick 只推进一次衰减的去重标记；不 setData。 */
    public static boolean beginDecayStep(ServerLevel level, BlockPos pos) {
        ChunkModules modules = data(level.getChunkAt(pos));
        long now = level.getGameTime();
        Long last = modules.getLastDecayTick(pos.asLong());
        if (last != null && last == now) {
            return false;
        }
        modules.putLastDecayTick(pos.asLong(), now);
        return true;
    }

    private static void migrateLegacy(ServerLevel level, BlockPos pos, ElibModuleContainer container) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS.get())) {
            return;
        }
        ChunkBlockElements legacy = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        StatusContainer old = legacy.peek(pos);
        if (old == null) {
            return;
        }
        container.put(old);
        legacy.removeContainerOnly(pos);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS.get(), legacy);
    }
}
