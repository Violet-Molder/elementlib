package com.linweiyun.elementlib.core.module.host;

import com.linweiyun.elementlib.core.module.ChunkModuleStore;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import com.linweiyun.elementlib.core.module.ElibModuleData;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKind;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKinds;
import com.linweiyun.elementlib.core.module.ElibModuleType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * 方块宿主：容器存在区块模块数据里。
 */
public final class BlockModuleHost implements ElibModuleHost {

    private final ServerLevel level;
    private final BlockPos pos;

    public BlockModuleHost(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Override
    public boolean isValid() {
        return level.isLoaded(pos);
    }

    @Override
    public ElibModuleTargetKind kind() {
        return ElibModuleTargetKinds.BLOCK;
    }

    @Override
    public String hostKey() {
        return "block:" + level.dimension().location() + "@" + pos.asLong();
    }

    @Override
    @Nullable
    public ElibModuleContainer container() {
        return isValid() ? ChunkModuleStore.container(level, pos) : null;
    }

    /** 瞬态容器：不落存档，区块重载即回初始值。 */
    public ElibModuleContainer transientContainer() {
        return ChunkModuleStore.transientContainer(level, pos);
    }

    @Override
    public void commit(ElibModuleContainer container) {
        ChunkModuleStore.commit(level, pos, container);
    }

    @Override
    @Nullable
    public <T extends ElibModuleData> T get(ElibModuleType<T> type) {
        ElibModuleContainer container = ChunkModuleStore.peek(level, pos);
        return container == null ? null : container.get(type);
    }

    @Override
    @Nullable
    public <T extends ElibModuleData> T ensure(ElibModuleType<T> type) {
        if (!type.supports(kind()) || !isValid()) {
            return null;
        }
        return ChunkModuleStore.container(level, pos).ensure(type);
    }

    @Override
    public ServerLevel level() {
        return level;
    }

    @Override
    public BlockPos blockPos() {
        return pos;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
