package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.module.ChunkModuleStore;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import com.linweiyun.elementlib.core.module.ElibModuleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * 方块元素容器的存取层；容器在区块模块数据里，水位仍在旧的 {@code chunk_elements} 附件上。
 */
public final class BlockElementStore {

    private BlockElementStore() {
    }

    /** 取元素容器（可写）；没有就现建，并会迁移旧存档。 */
    public static StatusContainer container(ServerLevel level, BlockPos pos) {
        return ChunkModuleStore.element(level, pos);
    }

    /** 只读查询：这个方块身上有没有挂过元素。 */
    @Nullable
    public static StatusContainer peek(ServerLevel level, BlockPos pos) {
        return ChunkModuleStore.peekElement(level, pos);
    }

    /** 记下这格水冻之前的水位（level），化回来时还原。 */
    public static void putWaterLevel(ServerLevel level, BlockPos pos, int waterLevel) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        elements.putWaterLevel(pos, waterLevel);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS.get(), elements);
    }

    /** 取出并清掉记录的水位；没有记录返回 null（那时按完整水源处理）。 */
    public static Integer takeWaterLevel(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = level.getChunkAt(pos);
        if (!chunk.hasData(ElementalAttachments.CHUNK_ELEMENTS.get())) {
            return null;
        }
        ChunkBlockElements elements = chunk.getData(ElementalAttachments.CHUNK_ELEMENTS);
        Integer waterLevel = elements.removeWaterLevel(pos);
        chunk.setData(ElementalAttachments.CHUNK_ELEMENTS.get(), elements);
        return waterLevel;
    }

    /** 这一格在本 game tick 是否还没推进过衰减。 */
    public static boolean beginDecayStep(ServerLevel level, BlockPos pos) {
        return ChunkModuleStore.beginDecayStep(level, pos);
    }

    /** 把元素容器的改动落回区块模块数据。 */
    public static void commit(ServerLevel level, BlockPos pos, StatusContainer container) {
        ElibModuleContainer modules = ChunkModuleStore.container(level, pos);
        if (modules.get(ElibModuleTypes.ELEMENT) == container) {
            return;
        }
        modules.put(container);
        ChunkModuleStore.commit(level, pos, modules);
    }

    /** 移除这一格的元素数据（元素容器 + 水位 + 衰减去重标记）。 */
    public static void clear(ServerLevel level, BlockPos pos) {
        ChunkModuleStore.clearElement(level, pos);
    }
}
