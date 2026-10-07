package com.linweiyun.elementlib.api.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.api.ElibAttackOutcome;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * 攻击行为事件（L1）—— 每次出手广播一条，与有没有打中无关；对空时 {@link #hostCount()} 为 0。
 *
 * <p>伤害管线内部的子步骤（{@code action.damageSubStep()} 为真）不发本事件。
 */
public final class ElibAttackPerformedEvent extends Event implements ElibIdentifiedEvent {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ElementLib.MOD_ID, "attack_performed");

    private final ServerLevel level;
    private final long gameTime;
    private final ElibAttackAction action;
    private final ElibAttackOutcome outcome;

    public ElibAttackPerformedEvent(ServerLevel level, long gameTime,
                                    ElibAttackAction action, ElibAttackOutcome outcome) {
        this.level = level;
        this.gameTime = gameTime;
        this.action = action;
        this.outcome = outcome;
    }

    @Override
    public ResourceLocation eventId() {
        return ID;
    }

    @Override
    public String displayName() {
        return "攻击行为";
    }

    public ServerLevel level() {
        return level;
    }

    public long gameTime() {
        return gameTime;
    }

    /** 这一次出手的完整描述：谁、朝哪、多远、哪种攻击、什么元素。 */
    public ElibAttackAction action() {
        return action;
    }

    /** 这一次出手的结果（只含这次几何收到的宿主；对空时为空）。 */
    public ElibAttackOutcome outcome() {
        return outcome;
    }

    /** 这一次出手触及的宿主数量；{@code 0} 表示对空。 */
    public int hostCount() {
        return outcome == null ? 0 : outcome.hosts().size();
    }

    @Override
    public String toString() {
        return "ElibAttackPerformedEvent[kind=" + (action == null ? "?" : action.kindId())
                + " trigger=" + (action == null ? "?" : action.trigger())
                + " hosts=" + hostCount() + "]";
    }
}