package com.linweiyun.elementlib.core.system.registry.register;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.api.ReactionCategory;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 示范反应类型 —— 注册全部反应类型，未实装反应的类型只有分类与翻译键。
 *
 * <p>整体受 {@code register-demo-reactions} 与 {@code register-demo-elements} 双门控：
 * 元素没注册时反应类型也不注册（id 的命名空间归属同一个库，缺元素就没有可反应的对象）。
 */
public final class ModReactionTypes {

    public static final DeferredRegister<ElementalReactionType> REACTION_TYPES = ModRegistries.REACTION_TYPES;

    // ===== 增幅 =====
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> MELT =
            register("melt", ReactionCategory.AMPLIFYING);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> VAPORIZE =
            register("vaporize", ReactionCategory.AMPLIFYING);

    // ===== 剧变 =====
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> SUPERCONDUCT =
            register("superconduct", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> SWIRL =
            register("swirl", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> ELECTRO_CHARGED =
            register("electro_charged", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> OVERLOAD =
            register("overload", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> BURNING =
            register("burning", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> BLOOM =
            register("bloom", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> HYPERBLOOM =
            register("hyperbloom", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> BURGEON =
            register("burgeon", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> QUICKEN =
            register("quicken", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> AGGRAVATE =
            register("aggravate", ReactionCategory.TRANSFORMATIVE);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> SPREAD =
            register("spread", ReactionCategory.TRANSFORMATIVE);

    // ===== 星体系 =====
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> STELLAR_SWIRL_WIND =
            register("stellar_swirl_wind", ReactionCategory.STELLAR);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> STELLAR_SWIRL_ICE =
            register("stellar_swirl_ice", ReactionCategory.STELLAR);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> STELLAR_CONDUCE_ELECTRO =
            register("stellar_conduce_electro", ReactionCategory.STELLAR);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> STELLAR_CONDUCE_ICE =
            register("stellar_conduce_ice", ReactionCategory.STELLAR);

    // ===== 月体系 =====
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> LUNAR_CHARGED =
            register("lunar_charged", ReactionCategory.LUNAR);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> LUNAR_BLOOM =
            register("lunar_bloom", ReactionCategory.LUNAR);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> LUNAR_CRYSTALLIZE =
            register("lunar_crystallize", ReactionCategory.LUNAR);

    // ===== 特殊 =====
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> FROZEN =
            register("frozen", ReactionCategory.SPECIAL);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> SHATTERED =
            register("shattered", ReactionCategory.SPECIAL);
    public static final DeferredHolder<ElementalReactionType, ElementalReactionType> CRYSTALLIZE =
            register("crystallize", ReactionCategory.SPECIAL);

    private ModReactionTypes() {
    }

    private static DeferredHolder<ElementalReactionType, ElementalReactionType> register(
            String path, ReactionCategory category) {
        return REACTION_TYPES.register(path,
                () -> new ElementalReactionType("reaction.minegenshin." + path, category));
    }

    /** 注册全部示范反应类型；任一门控关闭时什么都不注册。 */
    public static void register(IEventBus eventBus) {
        if (!ElementLibConfig.demoElementsEnabled() || !ElementLibConfig.demoReactionsEnabled()) {
            return;
        }
        REACTION_TYPES.register(eventBus);
    }
}
