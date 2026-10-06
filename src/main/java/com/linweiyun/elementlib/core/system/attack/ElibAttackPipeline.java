package com.linweiyun.elementlib.core.system.attack;

import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.api.ElibAttackBlockInterest;
import com.linweiyun.elementlib.api.ElibAttackElementResolver;
import com.linweiyun.elementlib.api.ElibAttackGate;
import com.linweiyun.elementlib.api.ElibAttackListener;
import com.linweiyun.elementlib.api.ElibAttackOutcome;
import com.linweiyun.elementlib.content.items.ElementSwordItem;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleHosts;
import com.linweiyun.elementlib.core.module.ElibModuleQuery;
import com.linweiyun.elementlib.core.module.ElibModuleTypes;
import com.linweiyun.elementlib.core.system.about.AttachContext;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.block.BlockElementHelper;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.about.block.BlockElementRules;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * <b>统一攻击入口</b> —— 对空、方块左键、实体左键、动作伤害点都从这里进。
 *
 * <pre>
 * dispatch(action)
 *   ├ 0. 门禁（只在服务端；模式门禁由使用方注入）
 *   ├ 1. COLLECT：按几何收集 ElibModuleHost
 *   ├ 2. ATTACH ：对每个宿主走元素附着入口（附着内触发反应）
 *   ├ 3. NOTIFY ：把 outcome 交给监听器（方块韧性、表现、消耗）
 *   └ 4. 返回 outcome：伤害侧用 attachResultOf(...) 读反应结果
 * </pre>
 */
public final class ElibAttackPipeline {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.COMBAT);

    private static final float BLOCK_GAUGE = AttachmentProfile.WEAK.getBaseQuantity();
    private static final float BLOCK_DECAY_PER_SECOND = AttachmentProfile.WEAK.getDecayPerSecond();

    private static final List<ElibAttackListener> LISTENERS = new CopyOnWriteArrayList<>();

    private static final List<ElibAttackBlockInterest> BLOCK_INTERESTS = new CopyOnWriteArrayList<>();

    private static volatile ElibAttackGate gate = action -> true;

    private static volatile ElibAttackElementResolver elementResolver = ElibAttackPipeline::resolveFromMainHand;

    private static volatile boolean elementAttachEnabled = true;

    private ElibAttackPipeline() {
    }

    public static ElibAttackOutcome dispatch(@Nullable ElibAttackAction action) {
        return dispatch(action, true);
    }

    /** @param checkGate false 时跳过门禁（伤害本身就只在原神模式里发生） */
    public static ElibAttackOutcome dispatch(@Nullable ElibAttackAction action, boolean checkGate) {
        if (action == null || !(action.attacker().level() instanceof ServerLevel level)) {
            return ElibAttackOutcome.empty(action);
        }
        if (checkGate && !gate.test(action)) {
            return ElibAttackOutcome.empty(action);
        }
        GenshinElement element = resolveElement(action);
        List<ElibModuleHost> hosts = ElibAttackGeometry.collect(
                level, action.attacker(), action.origin(), action.direction(), action.reach(),
                blockInterestOf(action, level, element));
        return runSweep(action, hosts, element);
    }

    /**
     * 以某点为中心的方块扫掠 —— 领域实体的持续伤害、下落攻击这类没有"挥击方向"的伤害用它。
     */
    public static ElibAttackOutcome dispatchAround(@Nullable ElibAttackAction action, @Nullable Vec3 center,
                                                   double radius, boolean checkGate) {
        if (action == null || center == null || !(action.attacker().level() instanceof ServerLevel level)) {
            return ElibAttackOutcome.empty(action);
        }
        if (checkGate && !gate.test(action)) {
            return ElibAttackOutcome.empty(action);
        }
        GenshinElement element = resolveElement(action);
        double r = Math.clamp(radius, 0.5, 16.0);
        List<ElibModuleHost> hosts = new ArrayList<>();
        for (ElibModuleHost host : ElibModuleQuery.in(
                level, AABB.ofSize(center, r * 2, r * 2, r * 2),
                blockInterestOf(action, level, element))) {
            if (host.blockPos() != null) {
                hosts.add(host);
            }
        }
        return runSweep(action, hosts, element);
    }

    private static ElibAttackOutcome runSweep(ElibAttackAction action, List<ElibModuleHost> hosts,
                                              @Nullable GenshinElement element) {
        ElibAttackOutcome outcome = new ElibAttackOutcome(action, hosts);
        // 附着前先记下每格的状态：下游的方块韧性按"冻结/迁移之前"判定
        for (ElibModuleHost host : hosts) {
            if (host.blockPos() != null && host.level() != null) {
                outcome.recordBlockState(host, host.level().getBlockState(host.blockPos()));
            }
        }

        if (elementAttachEnabled && element != null) {
            for (ElibModuleHost host : hosts) {
                if (!ElibModuleTypes.ELEMENT.supports(host.kind())) {
                    continue;
                }
                attachToHost(host, element, action, outcome, null);
            }
        }

        notifyListeners(outcome);
        return outcome;
    }

    /**
     * 精确落到一个实体目标上的攻击 —— 实体交互（左键、技能命中、伤害管线）都走这里。
     *
     * <p>扫掠 {@link #dispatch} 只处理方块；实体不用几何近似，避免给"范围里但没打中"的实体挂元素。
     */
    public static ElibAttackOutcome dispatchOn(@Nullable ElibAttackAction action, @Nullable Entity target) {
        return dispatchOn(action, target, null);
    }

    /**
     * @param reactionUnit 触发反应时使用的元素量；{@code null} 表示用实际附着量
     */
    public static ElibAttackOutcome dispatchOn(@Nullable ElibAttackAction action, @Nullable Entity target,
                                               @Nullable Float reactionUnit) {
        if (action == null || target == null || !(target.level() instanceof ServerLevel)) {
            return ElibAttackOutcome.empty(action);
        }
        if (!gate.test(action)) {
            return ElibAttackOutcome.empty(action);
        }
        ElibModuleHost host = ElibModuleHosts.of(target);
        if (host == null || !host.isValid()) {
            return ElibAttackOutcome.empty(action);
        }
        ElibAttackOutcome outcome = new ElibAttackOutcome(action, List.of(host));
        GenshinElement element = resolveElement(action);
        if (elementAttachEnabled && element != null && ElibModuleTypes.ELEMENT.supports(host.kind())) {
            attachToHost(host, element, action, outcome, reactionUnit);
        }
        notifyListeners(outcome);
        return outcome;
    }

    private static void attachToHost(ElibModuleHost host, GenshinElement element,
                                     ElibAttackAction action, ElibAttackOutcome outcome,
                                     @Nullable Float reactionUnit) {
        try {
            AttachResult result;
            if (host.blockPos() != null && host.level() != null) {
                // 方块附着量按这一下攻击带的元素量；0 = 这次攻击不附着
                if (action.elementAmount() <= 0f) {
                    return;
                }
                AttachmentProfile blockProfile = AttachmentProfile.forAmount(action.elementAmount());
                result = BlockElementHelper.applyElementAndGet(host.level(), host.blockPos(),
                        element, blockProfile.getBaseQuantity(), blockProfile.getDecayPerSecond());
            } else {
                ElementalHost elementalHost = elementalHostOf(host);
                if (elementalHost == null) {
                    return;
                }
                AttachContext context = reactionUnit == null
                        ? AttachContext.attack(action.sourceKey(), action.gameTime(), action.attacker(), 1.0f)
                        : new AttachContext(action.sourceKey(), action.gameTime(), action.attacker(), reactionUnit);
                result = ElementalAttachmentHelper.attach(
                        elementalHost, element, action.source(), action.profile(), context);
            }
            outcome.recordAttach(host, result);
        } catch (Exception e) {
            LOGGER.error("[ElementLib] 攻击附着失败 host={}", host.hostKey(), e);
        }
    }

    @Nullable
    private static GenshinElement resolveElement(ElibAttackAction action) {
        if (action.element() != null) {
            return action.element();
        }
        return elementResolver == null ? null : elementResolver.elementOf(action);
    }

    private static void notifyListeners(ElibAttackOutcome outcome) {
        for (ElibAttackListener listener : LISTENERS) {
            try {
                listener.onAttack(outcome);
            } catch (Exception e) {
                LOGGER.error("[ElementLib] 攻击监听器抛出异常", e);
            }
        }
    }

    public static void addListener(ElibAttackListener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(ElibAttackListener listener) {
        LISTENERS.remove(listener);
    }

    /** 注册方块关注点：方块韧性这类"只看某些方块"的模块用它。 */
    public static void addBlockInterest(ElibAttackBlockInterest interest) {
        if (interest != null) {
            BLOCK_INTERESTS.add(interest);
        }
    }

    public static void removeBlockInterest(ElibAttackBlockInterest interest) {
        BLOCK_INTERESTS.remove(interest);
    }

    /** 模式门禁；传 {@code null} 恢复「永远放行」。 */
    public static void setGate(@Nullable ElibAttackGate newGate) {
        gate = newGate == null ? action -> true : newGate;
    }

    /** 元素解析器；传 {@code null} 表示「不解析，只有显式带元素的攻击才附着」。 */
    public static void setElementResolver(@Nullable ElibAttackElementResolver resolver) {
        elementResolver = resolver;
    }

    /** 关掉后管线只发事件、不做附着。 */
    public static void setElementAttachEnabled(boolean enabled) {
        elementAttachEnabled = enabled;
    }

    @Nullable
    private static ElementalHost elementalHostOf(ElibModuleHost host) {
        if (host.entity() != null) {
            return EntityHost.of(host.entity());
        }
        if (host.level() != null && host.blockPos() != null) {
            return BlockHost.of(host.level(), host.blockPos());
        }
        return null;
    }

    private static ElibModuleQuery.BlockInterest blockInterestOf(ElibAttackAction action, ServerLevel level,
                                                                @Nullable GenshinElement element) {
        return (pos, state) -> {
            for (ElibAttackBlockInterest interest : BLOCK_INTERESTS) {
                if (interest.interested(action, level, pos, state)) {
                    return true;
                }
            }
            return element != null && BlockElementRules.accepts(state, element);
        };
    }

    /** 默认元素来源：主手拿着 elementlib 的元素剑。 */
    @Nullable
    private static GenshinElement resolveFromMainHand(ElibAttackAction action) {
        if (!(action.attacker() instanceof LivingEntity living)) {
            return null;
        }
        ItemStack stack = living.getMainHandItem();
        return stack.getItem() instanceof ElementSwordItem sword ? sword.element() : null;
    }
}
