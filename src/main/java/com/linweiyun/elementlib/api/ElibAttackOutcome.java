package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一次攻击的结果：被触及的宿主、每个宿主的 {@link AttachResult}。
 *
 * <p>攻击阶段与伤害阶段分家后，伤害侧用 {@link #attachResultOf(Entity)} 读反应倍率，
 * 不再自己附着一次。
 */
public final class ElibAttackOutcome {

    private final ElibAttackAction action;
    private final List<ElibModuleHost> hosts;
    private final Map<String, AttachResult> attachResults;
    private final Map<String, BlockState> blockStates;

    public ElibAttackOutcome(@Nullable ElibAttackAction action, List<ElibModuleHost> hosts) {
        this.action = action;
        this.hosts = hosts;
        this.attachResults = new LinkedHashMap<>();
        this.blockStates = new LinkedHashMap<>();
    }

    public static ElibAttackOutcome empty(@Nullable ElibAttackAction action) {
        return new ElibAttackOutcome(action, new ArrayList<>());
    }

    @Nullable
    public ElibAttackAction action() {
        return action;
    }

    public List<ElibModuleHost> hosts() {
        return Collections.unmodifiableList(hosts);
    }

    public Map<String, AttachResult> attachResults() {
        return Collections.unmodifiableMap(attachResults);
    }

    @Nullable
    public AttachResult attachResultOf(ElibModuleHost host) {
        return attachResults.get(host.hostKey());
    }

    /** 伤害侧按实体取这次攻击在它身上留下的附着结果。 */
    @Nullable
    public AttachResult attachResultOf(Entity entity) {
        return attachResults.get("entity:" + entity.getUUID());
    }

    /** 攻击管线内部使用。 */
    public void recordAttach(ElibModuleHost host, AttachResult result) {
        attachResults.put(host.hostKey(), result);
    }

    /** 攻击管线内部使用：记下附着发生前这一格的方块状态。 */
    public void recordBlockState(ElibModuleHost host, BlockState state) {
        blockStates.put(host.hostKey(), state);
    }

    /** 附着发生前这一格的状态；没有记录返回 {@code null}。 */
    @Nullable
    public BlockState blockStateOf(ElibModuleHost host) {
        return blockStates.get(host.hostKey());
    }

    public boolean isEmpty() {
        return hosts.isEmpty();
    }
}
