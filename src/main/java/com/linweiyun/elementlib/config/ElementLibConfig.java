package com.linweiyun.elementlib.config;

import com.linweiyun.elementlib.config.reaction.ReactionConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 三个配置文件的 Spec 与全库统一的读取入口。
 */
public final class ElementLibConfig {

    public static final ModConfigSpec ELEMENTS_SPEC;
    public static final ModConfigSpec REACTIONS_SPEC;
    public static final ModConfigSpec ITEMS_SPEC;

    /** 反应配置的 Spec（等价于 {@link #REACTIONS_SPEC}）。 */
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue REGISTER_DEMO_ELEMENTS;
    /** 附着图标总闸（命令可直接改写）。 */
    public static ModConfigSpec.BooleanValue SHOW_AURA_ICON;
    private static final ModConfigSpec.BooleanValue REGISTER_DEMO_REACTIONS;
    private static final ModConfigSpec.BooleanValue REGISTER_DEMO_ITEMS;

    /** 接入方强制关闭示范内容（在 {@code ElementLib} 构造之前调用）。 */
    private static volatile boolean demoSuppressed;

    private static final ModConfigSpec.DoubleValue DAMAGE_SUPERCONDUCT;
    private static final ModConfigSpec.DoubleValue DAMAGE_ELECTRO_CHARGED;
    private static final ModConfigSpec.DoubleValue DAMAGE_SWIRL;
    private static final ModConfigSpec.DoubleValue DAMAGE_STELLAR_SWIRL_WIND;
    private static final ModConfigSpec.DoubleValue DAMAGE_STELLAR_SWIRL_ICE;
    private static final ModConfigSpec.DoubleValue DAMAGE_STELLAR_CONDUCE_ELECTRO;
    private static final ModConfigSpec.DoubleValue DAMAGE_STELLAR_CONDUCE_ICE;
    private static final ModConfigSpec.DoubleValue DAMAGE_LUNAR_CHARGED;

    static {
        ModConfigSpec.Builder elements = new ModConfigSpec.Builder();
        REGISTER_DEMO_ELEMENTS = elements.comment("是否注册示范元素（关闭后元素、反应、方块环境规则整套都不注册）")
                .define("register-demo-elements", true);
        SHOW_AURA_ICON = elements.comment("是否在生物头顶渲染元素附着图标")
                .define("show-aura-icon", true);
        ELEMENTS_SPEC = elements.build();

        ModConfigSpec.Builder reactions = new ModConfigSpec.Builder();
        REGISTER_DEMO_REACTIONS = reactions.comment("是否注册示范反应类型与示范反应")
                .define("register-demo-reactions", true);
        ReactionConfig.register(reactions);
        DAMAGE_SUPERCONDUCT = reactions.comment("超导示范伤害")
                .defineInRange("superconduct", 10.0, 0.0, 1.0E7);
        DAMAGE_ELECTRO_CHARGED = reactions.comment("感电示范伤害")
                .defineInRange("electro_charged", 12.0, 0.0, 1.0E7);
        DAMAGE_SWIRL = reactions.comment("扩散示范伤害")
                .defineInRange("swirl", 14.0, 0.0, 1.0E7);
        DAMAGE_STELLAR_SWIRL_WIND = reactions.comment("星扩散·风示范伤害")
                .defineInRange("stellar_swirl_wind", 20.0, 0.0, 1.0E7);
        DAMAGE_STELLAR_SWIRL_ICE = reactions.comment("星扩散·冰爆炸示范伤害")
                .defineInRange("stellar_swirl_ice", 30.0, 0.0, 1.0E7);
        DAMAGE_STELLAR_CONDUCE_ELECTRO = reactions.comment("星超导·雷示范伤害")
                .defineInRange("stellar_conduce_electro", 24.0, 0.0, 1.0E7);
        DAMAGE_STELLAR_CONDUCE_ICE = reactions.comment("星超导·冰示范伤害")
                .defineInRange("stellar_conduce_ice", 28.0, 0.0, 1.0E7);
        DAMAGE_LUNAR_CHARGED = reactions.comment("月感电示范伤害（应明显高于感电）")
                .defineInRange("lunar_charged", 40.0, 0.0, 1.0E7);
        REACTIONS_SPEC = reactions.build();

        ModConfigSpec.Builder items = new ModConfigSpec.Builder();
        REGISTER_DEMO_ITEMS = items.comment("是否注册示范物品")
                .define("register-demo-items", true);
        ITEMS_SPEC = items.build();

        SPEC = REACTIONS_SPEC;
    }

    private ElementLibConfig() {
    }

    /**
     * 强制关闭全部示范内容（元素 / 反应 / 反应类型 / 物品 / 实体），配置文件不再生效。
     *
     * <p>接入方注册自己的元素时调用，避免示范内容与本体内容同时存在。
     */
    public static void suppressDemoContent() {
        demoSuppressed = true;
        ElementLibEarlyFlags.resync(false, false, false, ElementLibEarlyFlags.auraIcon());
    }

    public static boolean demoElementsEnabled() {
        return !demoSuppressed && ElementLibEarlyFlags.demoElements();
    }

    public static boolean demoReactionsEnabled() {
        return !demoSuppressed && ElementLibEarlyFlags.demoReactions();
    }

    public static boolean demoItemsEnabled() {
        return !demoSuppressed && ElementLibEarlyFlags.demoItems();
    }

    /** 附着图标总闸；配置尚未加载时返回默认 {@code true}，绝不抛异常。 */
    public static boolean showAuraIcon() {
        try {
            return SHOW_AURA_ICON == null || !ELEMENTS_SPEC.isLoaded() || SHOW_AURA_ICON.get();
        } catch (RuntimeException e) {
            return true;
        }
    }

    /**
     * 示范伤害值 —— 按反应 key 取值，未登记的 key 返回 {@code 0}。
     *
     * <p>配置未加载时退回 {@code reactions.toml} 里写的默认值。
     */
    public static float baseDamage(String key) {
        if (key == null) return 0f;
        return switch (key) {
            case "superconduct" -> damage(DAMAGE_SUPERCONDUCT, 10f);
            case "electro_charged" -> damage(DAMAGE_ELECTRO_CHARGED, 12f);
            case "swirl" -> damage(DAMAGE_SWIRL, 14f);
            case "stellar_swirl_wind" -> damage(DAMAGE_STELLAR_SWIRL_WIND, 20f);
            case "stellar_swirl_ice" -> damage(DAMAGE_STELLAR_SWIRL_ICE, 30f);
            case "stellar_conduce_electro" -> damage(DAMAGE_STELLAR_CONDUCE_ELECTRO, 24f);
            case "stellar_conduce_ice" -> damage(DAMAGE_STELLAR_CONDUCE_ICE, 28f);
            case "lunar_charged" -> damage(DAMAGE_LUNAR_CHARGED, 40f);
            default -> 0f;
        };
    }

    /** 配置加载后把 Spec 的真实值同步给 {@link ElementLibEarlyFlags}。 */
    public static void syncEarlyFlags() {
        if (demoSuppressed) {
            ElementLibEarlyFlags.resync(false, false, false, ElementLibEarlyFlags.auraIcon());
            return;
        }
        ElementLibEarlyFlags.resync(
                booleanValue(ELEMENTS_SPEC, REGISTER_DEMO_ELEMENTS, ElementLibEarlyFlags.demoElements()),
                booleanValue(REACTIONS_SPEC, REGISTER_DEMO_REACTIONS, ElementLibEarlyFlags.demoReactions()),
                booleanValue(ITEMS_SPEC, REGISTER_DEMO_ITEMS, ElementLibEarlyFlags.demoItems()),
                booleanValue(ELEMENTS_SPEC, SHOW_AURA_ICON, ElementLibEarlyFlags.auraIcon()));
    }

    private static boolean booleanValue(ModConfigSpec spec, ModConfigSpec.BooleanValue value, boolean fallback) {
        if (spec == null || value == null || !spec.isLoaded()) {
            return fallback;
        }
        Boolean read = value.get();
        return read != null ? read : fallback;
    }

    private static float damage(ModConfigSpec.DoubleValue value, float fallback) {
        if (value == null || !REACTIONS_SPEC.isLoaded()) {
            return fallback;
        }
        Double read = value.get();
        return read != null ? read.floatValue() : fallback;
    }
}
