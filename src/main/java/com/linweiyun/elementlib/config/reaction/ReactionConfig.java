package com.linweiyun.elementlib.config.reaction;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 反应倍率配置 —— 只含已实装反应用到的倍率。
 */
public final class ReactionConfig {

    // ===== 增幅反应 =====
    /** 融化（后手克制方：火打冰）倍率。 */
    public static ModConfigSpec.DoubleValue MELT;
    /** 融化（后手被克制方：冰打火）倍率。 */
    public static ModConfigSpec.DoubleValue MELT_NEGATIVE;

    /** 蒸发（后手克制方：火打水）倍率。 */
    public static ModConfigSpec.DoubleValue VAPORIZE;
    /** 蒸发（后手被克制方：水打火）倍率。 */
    public static ModConfigSpec.DoubleValue VAPORIZE_NEGATIVE;

    // ===== 剧变反应 =====
    /** 超导倍率。 */
    public static ModConfigSpec.DoubleValue SUPERCONDUCT;
    /** 扩散倍率。 */
    public static ModConfigSpec.DoubleValue SWIRL;
    /** 感电倍率。 */
    public static ModConfigSpec.DoubleValue ELECTROCHARGED;

    private ReactionConfig() {
    }

    public static void register(ModConfigSpec.Builder builder) {
        builder.push("reaction-amplifying");
        MELT = builder.comment("融化：后手克制方（火打冰）倍率")
                .defineInRange("reaction-melt", 2.0, 0.0, 100.0);
        MELT_NEGATIVE = builder.comment("融化：后手被克制方（冰打火）倍率")
                .defineInRange("reaction-melt-negative", 1.5, 0.0, 100.0);
        VAPORIZE = builder.comment("蒸发：后手克制方（火打水）倍率")
                .defineInRange("reaction-vaporize", 2.0, 0.0, 100.0);
        VAPORIZE_NEGATIVE = builder.comment("蒸发：后手被克制方（水打火）倍率")
                .defineInRange("reaction-vaporize-negative", 1.5, 0.0, 100.0);
        builder.pop();

        builder.push("reaction-fusion");
        SUPERCONDUCT = builder.comment("超导倍率")
                .defineInRange("reaction-superconduct", 1.0, 0.0, 100.0);
        SWIRL = builder.comment("扩散倍率")
                .defineInRange("reaction-swirl", 1.2, 0.0, 100.0);
        ELECTROCHARGED = builder.comment("感电倍率")
                .defineInRange("reaction-electrocharged", 1.2, 0.0, 100.0);
        builder.pop();
    }
}
