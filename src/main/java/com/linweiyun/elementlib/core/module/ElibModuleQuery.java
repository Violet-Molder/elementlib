package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.core.module.host.BlockModuleHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * <b>统一查询入口</b> —— 「一次性拿到周围所有这个模块」。
 *
 * <p>只在服务端做权威结果；实体按 {@link ElibModuleHost#isValid()} 过滤。
 */
public final class ElibModuleQuery {

    /** 方块筛选：返回 true 才把这一格收进来（避免为没人要的方块建容器）。 */
    @FunctionalInterface
    public interface BlockInterest {
        boolean interested(BlockPos pos, BlockState state);
    }

    private static final int MAX_BLOCK_VOLUME = 4096;

    private ElibModuleQuery() {
    }

    public static List<ElibModuleHost> around(Level level, Vec3 center, double radius) {
        double r = Math.max(0.0, radius);
        return in(level, AABB.ofSize(center, r * 2.0, r * 2.0, r * 2.0));
    }

    public static List<ElibModuleHost> in(Level level, AABB box) {
        return in(level, box, null);
    }

    public static List<ElibModuleHost> in(Level level, AABB box, @Nullable BlockInterest blockInterest) {
        List<ElibModuleHost> hosts = new ArrayList<>();
        if (!(level instanceof ServerLevel serverLevel)) {
            return hosts;
        }
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, box)) {
            ElibModuleHost host = ElibModuleHosts.of(entity);
            if (host != null && host.isValid()) {
                hosts.add(host);
            }
        }
        collectBlocks(serverLevel, box, blockInterest, hosts);
        return hosts;
    }

    /** 直接拿数据对象；没有这份数据或不支持该类型的宿主会被剔除。 */
    public static <T extends ElibModuleData> List<T> dataIn(Level level, AABB box, ElibModuleType<T> type) {
        List<T> data = new ArrayList<>();
        for (ElibModuleHost host : in(level, box)) {
            if (!type.supports(host.kind())) {
                continue;
            }
            T value = host.get(type);
            if (value != null) {
                data.add(value);
            }
        }
        return data;
    }

    private static void collectBlocks(ServerLevel level, AABB box, @Nullable BlockInterest interest,
                                      List<ElibModuleHost> hosts) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);
        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume <= 0 || volume > MAX_BLOCK_VOLUME) {
            return;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockPos pos = cursor.immutable();
                    ElibModuleContainer existing = ChunkModuleStore.peek(level, pos);
                    boolean hasData = existing != null && !existing.isEmpty();
                    if (!hasData) {
                        BlockState state = level.getBlockState(pos);
                        if (interest == null || !interest.interested(pos, state)) {
                            continue;
                        }
                    }
                    hosts.add(new BlockModuleHost(level, pos));
                }
            }
        }
    }
}
