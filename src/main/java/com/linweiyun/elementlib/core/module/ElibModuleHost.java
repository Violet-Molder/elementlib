package com.linweiyun.elementlib.core.module;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * <b>模块宿主</b> —— 「模块挂在什么东西上」的唯一抽象。
 *
 * <p>行为层只面对它，不再先判断"是实体还是方块"。
 */
public interface ElibModuleHost {

    /** 宿主还有效吗（实体活着 / 方块所在区块已加载 / 栈还存在）。 */
    boolean isValid();

    ElibModuleTargetKind kind();

    /** 可写容器；无效宿主返回 {@code null}。 */
    @Nullable
    ElibModuleContainer container();

    /** 宿主标识，用于日志与去重。 */
    String hostKey();

    /** 只读：没有这份数据就返回 {@code null}。 */
    @Nullable
    <T extends ElibModuleData> T get(ElibModuleType<T> type);

    /** 读取；没有就懒建（宿主支持的模块类型才会建）。 */
    @Nullable
    <T extends ElibModuleData> T ensure(ElibModuleType<T> type);

    /** 把容器改动落回宿主存储（实体附件 / Chunk 数据 / 物品组件 / 角色数据）。 */
    void commit(ElibModuleContainer container);

    @Nullable
    default LivingEntity entity() {
        return null;
    }

    @Nullable
    default ServerLevel level() {
        return null;
    }

    @Nullable
    default BlockPos blockPos() {
        return null;
    }

    @Nullable
    default ItemStack itemStack() {
        return null;
    }
}
