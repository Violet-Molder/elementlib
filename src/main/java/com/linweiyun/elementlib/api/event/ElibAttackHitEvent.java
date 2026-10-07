package com.linweiyun.elementlib.api.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.api.ElibAttackOutcome;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * 击中目标事件（L2）—— 这一下碰到实体或方块时，对每个被触及的宿主各广播一条。
 *
 * <p>{@link #targetEntity()} 与 {@link #targetBlock()} 哪个非空表示宿主是实体还是方块；
 * 命中不等于附着成功，没挂上时 {@link #attach()} 为 {@code null} 或 {@code attached()} 为假。
 */
public final class ElibAttackHitEvent extends Event implements ElibIdentifiedEvent {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "attack_hit");

    private final ServerLevel level;
    private final long gameTime;
    private final ElibAttackAction action;
    private final ElibAttackOutcome outcome;
    private final ElibModuleHost host;
    @Nullable private final AttachResult attach;
    private final boolean blockChanged;

    public ElibAttackHitEvent(ServerLevel level, long gameTime,
                              ElibAttackAction action, ElibAttackOutcome outcome,
                              ElibModuleHost host, @Nullable AttachResult attach,
                              boolean blockChanged) {
        this.level = level;
        this.gameTime = gameTime;
        this.action = action;
        this.outcome = outcome;
        this.host = host;
        this.attach = attach;
        this.blockChanged = blockChanged;
    }

    @Override
    public Identifier eventId() {
        return ID;
    }

    @Override
    public String displayName() {
        return "击中目标";
    }

    public ServerLevel level() {
        return level;
    }

    public long gameTime() {
        return gameTime;
    }

    public ElibAttackAction action() {
        return action;
    }

    public ElibAttackOutcome outcome() {
        return outcome;
    }

    /** 被打到的那个宿主。 */
    public ElibModuleHost host() {
        return host;
    }

    /** 被打到的实体；方块宿主时为 {@code null}。 */
    @Nullable
    public Entity targetEntity() {
        return host.entity();
    }

    /** 被打到的方块坐标；实体宿主时为 {@code null}。 */
    @Nullable
    public BlockPos targetBlock() {
        return host.blockPos();
    }

    /** 这一下在它身上挂上了什么（含反应结果）；没有尝试附着时为 {@code null}。 */
    @Nullable
    public AttachResult attach() {
        return attach;
    }

    /** 方块宿主：附着前后方块状态是否变了（元素迁移的结果）。实体宿主恒为 {@code false}。 */
    public boolean blockChanged() {
        return blockChanged;
    }

    @Override
    public String toString() {
        return "ElibAttackHitEvent[host=" + host.hostKey()
                + " entity=" + (targetEntity() != null) + " block=" + targetBlock() + "]";
    }
}