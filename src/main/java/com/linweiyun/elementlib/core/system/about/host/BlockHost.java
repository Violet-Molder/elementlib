package com.linweiyun.elementlib.core.system.about.host;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.block.BlockElementRules;
import com.linweiyun.elementlib.core.system.about.block.BlockElementStore;
import com.linweiyun.elementlib.core.system.about.block.BlockSelfAura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * <b>方块宿主</b> —— {@link ElementalHost} 在「一个坐标处的方块」上的实现。
 * 容器来自 {@link BlockElementStore}，能否附着由 {@link BlockElementRules} 的规则表回答，
 * 之后的反应与实体侧走同一个 {@code ElementalReactionManager}。
 *
 * <p>刻意拿的是 {@link BlockState}（不是 {@code BlockEntity}）—— 绝大多数可附着方块（水、冰）没有方块实体。
 */
public final class BlockHost implements ElementalHost {

    private final ServerLevel level;
    private final BlockPos pos;

    private BlockHost(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /** 包一个方块坐标；{@code level == null} 时返回 {@code null}。 */
    @Nullable
    public static BlockHost of(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return (level == null || pos == null) ? null : new BlockHost(level, pos);
    }

    @Override
    public boolean isValid() {
        return level.isLoaded(pos);
    }

    @Override
    public StatusContainer container() {
        if (!isValid()) {
            return null;
        }
        StatusContainer container = BlockElementStore.container(level, pos);
        // 环境自附着：这个方块「本来就是水/冰」——反应要靠它当先手，缺了反应就无从发生
        BlockSelfAura.ensure(this, container, state());
        return container;
    }

    /**
     * <b>只读</b>取容器：不建、不补自附着、不落盘。
     *
     * <p>与 {@link #container()} 的区别：{@link #container()} 是写路径，会现建容器、会补水/冰自带的元素；
     * 本方法是读路径，只看现有事实。推进衰减、判断状态这类高频只读逻辑一律用它。
     */
    @Nullable
    public StatusContainer peekContainer() {
        return isValid() ? BlockElementStore.peek(level, pos) : null;
    }

    @Override
    public String hostKey() {
        return "block:" + level.dimension().identifier() + "@" + pos.asLong();
    }

    @Override
    public boolean acceptsElement(GenshinElement element, AttachmentSource source,
                                  AttachmentProfile profile) {
        return BlockElementRules.accepts(state(), element);
    }

    /**
     * 方块侧元素钩子 —— 空实现。
     *
     * <p>方块吃元素后的「样子变化」（水结冰、冰化水）由状态迁移消费者统一读容器后决定：
     * 方块的状态是表现，元素附着是原因，两者不互相写死。
     */
    @Override
    public void onElementAttached(GenshinElement element) {
    }

    @Override
    public void onElementDetached(GenshinElement element) {
    }

    /** 当前坐标上的方块状态。 */
    public BlockState state() {
        return level.getBlockState(pos);
    }

    /** 把容器改动写回 Chunk 数据（相当于实体宿主的 {@code setData}）。 */
    public void commit(StatusContainer container) {
        BlockElementStore.commit(level, pos, container);
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
