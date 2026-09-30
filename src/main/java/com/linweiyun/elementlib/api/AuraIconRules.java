package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 元素附着图标的显示规则表：按元素、按来源注册条件，外加环境附着的默认隐藏与豁免名单。
 */
public final class AuraIconRules {

    /** 与其它条件取「与」的元素条件。 */
    private static final List<AuraIconCondition> ELEMENT_CONDITIONS = new CopyOnWriteArrayList<>();

    /** 按来源分的条件，只在该来源的附着上判定。 */
    private static final Map<AttachmentSource, List<AuraIconCondition>> SOURCE_CONDITIONS =
            new ConcurrentHashMap<>();

    /** 环境隐藏规则的豁免名单：元素 → 仍然要画图标的实体类型。 */
    private static final Map<GenshinElement, Set<EntityType<?>>> ENVIRONMENT_EXEMPT =
            new ConcurrentHashMap<>();

    /** 演示元素可用时才有水元素，环境默认规则也就只在这种情况下安装。 */
    private static final boolean ENVIRONMENT_DEFAULT_INSTALLED;

    static {
        GenshinElement hydro = ElementLibConfig.demoElementsEnabled()
                ? ModElements.of(ModElements.HYDRO)
                : null;
        if (hydro != null) {
            // 环境附着的水对水生生物不显示图标。
            // 豁免名单可继续加：复制这一行、换成别的实体类型即可。
            registerEnvironmentExempt(hydro, EntityTypes.DROWNED);
        }
        ENVIRONMENT_DEFAULT_INSTALLED = hydro != null;
    }

    private AuraIconRules() {
    }

    /** 按元素附加条件（与其它条件取「与」）。 */
    public static void register(AuraIconCondition condition) {
        if (condition == null) {
            return;
        }
        ELEMENT_CONDITIONS.add(condition);
    }

    /** 按来源附加条件。 */
    public static void registerForSource(AttachmentSource source, AuraIconCondition condition) {
        if (source == null || condition == null) {
            return;
        }
        SOURCE_CONDITIONS.computeIfAbsent(source, s -> new CopyOnWriteArrayList<>()).add(condition);
    }

    /**
     * 环境附着豁免名单：这些实体类型上，该元素的<b>环境</b>附着仍然显示图标。
     *
     * <p>元素为 {@code null}（未注册）时调用无效果。
     */
    public static void registerEnvironmentExempt(@Nullable GenshinElement element, EntityType<?>... types) {
        if (element == null || types == null || types.length == 0) {
            return;
        }
        Set<EntityType<?>> exempt = ENVIRONMENT_EXEMPT.computeIfAbsent(
                element, e -> ConcurrentHashMap.newKeySet());
        for (EntityType<?> type : types) {
            if (type != null) {
                exempt.add(type);
            }
        }
    }

    /** 该元素的环境附着在这个实体类型上是否被豁免（豁免 = 照常显示图标）。 */
    public static boolean isEnvironmentExempt(@Nullable GenshinElement element,
                                              @Nullable EntityType<?> type) {
        if (element == null || type == null) {
            return false;
        }
        Set<EntityType<?>> exempt = ENVIRONMENT_EXEMPT.get(element);
        return exempt != null && exempt.contains(type);
    }

    /** 完整判定：总闸 → 元素钩子 → 元素条件 → 来源条件 → 环境默认规则（含豁免名单）。 */
    public static boolean shouldShow(@Nullable AuraIconContext ctx) {
        if (ctx == null) {
            return false;
        }
        if (!ElementLibApi.auraIconVisible()) {
            return false;
        }
        if (!ElementLibConfig.showAuraIcon()) {
            return false;
        }

        GenshinElement element = ctx.element();
        if (element == null) {
            return false;
        }

        if (!element.shouldShowAuraIcon(ctx)) {
            return false;
        }

        for (AuraIconCondition condition : ELEMENT_CONDITIONS) {
            if (!condition.shouldShow(ctx)) {
                return false;
            }
        }

        if (ctx.source() != null) {
            List<AuraIconCondition> bySource = SOURCE_CONDITIONS.get(ctx.source());
            if (bySource != null) {
                for (AuraIconCondition condition : bySource) {
                    if (!condition.shouldShow(ctx)) {
                        return false;
                    }
                }
            }
        }

        return !isHiddenByEnvironmentDefault(ctx);
    }

    /** 环境附着的默认隐藏：水在 {@code WATER_CREATURE} / {@code WATER_AMBIENT} 上不画，除非在豁免名单里。 */
    private static boolean isHiddenByEnvironmentDefault(AuraIconContext ctx) {
        if (!ENVIRONMENT_DEFAULT_INSTALLED) {
            return false;
        }
        if (ctx.source() != AttachmentSource.ENVIRONMENTAL) {
            return false;
        }
        if (!ModElements.is(ctx.element(), ModElements.HYDRO)) {
            return false;
        }

        LivingEntity entity = ctx.entity();
        if (entity == null) {
            return false;
        }
        MobCategory category = entity.getType().getCategory();
        if (category != MobCategory.WATER_CREATURE && category != MobCategory.WATER_AMBIENT) {
            return false;
        }
        return !isEnvironmentExempt(ctx.element(), entity.getType());
    }
}
