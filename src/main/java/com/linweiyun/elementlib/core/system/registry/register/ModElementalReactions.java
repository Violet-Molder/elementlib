package com.linweiyun.elementlib.core.system.registry.register;

import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.ElectroChargedReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.FreezeReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.LunarChargedReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.MeltReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.SuperConductReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.SwirlReaction;
import com.linweiyun.elementlib.core.system.reaction.builtin.VaporizeReaction;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;

/**
 * 示范反应的注册：蒸发 / 融化 / 冻结 / 扩散 / 感电 / 超导 / 月感电。
 *
 * <p>只在「示范元素 + 示范反应」同时开启时注册；关闭时各字段为 {@code null}，取用前必须判空。
 * 元素 id 使用本模组命名空间。
 */
public class ModElementalReactions {

    public static final DeferredRegister<ElementalReaction> ELEMENTAL_REACTIONS = ModRegistries.ELEMENTAL_REACTIONS;

    private static final boolean ENABLED =
            ElementLibConfig.demoReactionsEnabled() && ElementLibConfig.demoElementsEnabled();

    /** 蒸发：水:火 = 1:2，火克水。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, VaporizeReaction> VAPORIZE = ENABLED
            ? ELEMENTAL_REACTIONS.register("vaporize", () -> new VaporizeReaction(
                    ModReactionTypes.VAPORIZE,
                    "elementlib:hydro", "elementlib:pyro",
                    1f, 2f,
                    0))
            : null;

    /** 融化：火:冰 = 1:2，火克冰（冻通过 getMainElement 归并到冰）。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, MeltReaction> MELT = ENABLED
            ? ELEMENTAL_REACTIONS.register("melt", () -> new MeltReaction(
                    ModReactionTypes.MELT,
                    "elementlib:pyro", "elementlib:cyro",
                    1f, 2f,
                    0))
            : null;

    /** 冻结：水:冰 = 1:1。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, FreezeReaction> FREEZE = ENABLED
            ? ELEMENTAL_REACTIONS.register("freeze", () -> new FreezeReaction(
                    ModReactionTypes.FROZEN,
                    "elementlib:hydro", "elementlib:cyro",
                    1f, 1f,
                    0))
            : null;

    /** 扩散：火:风 = 1:2（风被克制），剧变反应。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, SwirlReaction> SWIRL = ENABLED
            ? ELEMENTAL_REACTIONS.register("swirl", () -> new SwirlReaction(
                    ModReactionTypes.SWIRL,
                    "elementlib:pyro", "elementlib:anemo",
                    1f, 2f,
                    5))
            : null;

    /** 感电：水:雷 = 1:1，共存反应。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, ElectroChargedReaction> ELECTRO_CHARGED = ENABLED
            ? ELEMENTAL_REACTIONS.register("electro_charged", () -> new ElectroChargedReaction(
                    ModReactionTypes.ELECTRO_CHARGED,
                    "elementlib:hydro", "elementlib:electro",
                    1f, 1f,
                    0))
            : null;

    /** 超导：雷:冰 = 1:1，剧变反应，伤害冷却 10 刻。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, SuperConductReaction> SUPERCONDUCT = ENABLED
            ? ELEMENTAL_REACTIONS.register("superconduct", () -> new SuperConductReaction(
                    ModReactionTypes.SUPERCONDUCT,
                    "elementlib:electro", "elementlib:cyro",
                    1f, 1f,
                    0))
            : null;

    /** 月感电：水:雷 = 1:1，优先级走默认表（排在感电之前）。 */
    @Nullable
    public static final DeferredHolder<ElementalReaction, LunarChargedReaction> LUNAR_CHARGED = ENABLED
            ? ELEMENTAL_REACTIONS.register("lunar_charged", () -> new LunarChargedReaction(
                    ModReactionTypes.LUNAR_CHARGED,
                    "elementlib:hydro", "elementlib:electro",
                    1f, 1f,
                    -1))
            : null;

    private ModElementalReactions() {
    }

    /** 示范内容关闭时整个注册器不挂到事件总线上。 */
    public static void register(IEventBus eventBus) {
        if (!ENABLED) {
            return;
        }
        ELEMENTAL_REACTIONS.register(eventBus);
    }
}
