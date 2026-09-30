package com.linweiyun.elementlib.core.entity;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.config.ElementLibConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * 示范实体的实体类型注册：{@code thunder_cloud} / {@code stellar_vortex}。
 *
 * <p>两个实体类型都只在「示范元素 + 示范反应」同时开启时注册；关闭时
 * {@link #THUNDER_CLOUD} / {@link #STELLAR_VORTEX} 保持 {@code null}，取用前必须判空。
 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ElementLib.MOD_ID);

    private static final boolean ENABLED =
            ElementLibConfig.demoElementsEnabled() && ElementLibConfig.demoReactionsEnabled();

    @Nullable
    public static final DeferredHolder<EntityType<?>, EntityType<ThunderCloudEntity>> THUNDER_CLOUD =
            ENABLED ? registerThunderCloud() : null;

    @Nullable
    public static final DeferredHolder<EntityType<?>, EntityType<StellarVortexEntity>> STELLAR_VORTEX =
            ENABLED ? registerStellarVortex() : null;

    private ModEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<ThunderCloudEntity>> registerThunderCloud() {
        return ENTITIES.register(
                "thunder_cloud",
                () -> EntityType.Builder.<ThunderCloudEntity>of(ThunderCloudEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .clientTrackingRange(8)
                        .updateInterval(20)
                        .build(ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                ElementLib.id("thunder_cloud"))
                        ));
    }

    private static DeferredHolder<EntityType<?>, EntityType<StellarVortexEntity>> registerStellarVortex() {
        return ENTITIES.register(
                "stellar_vortex",
                () -> EntityType.Builder.<StellarVortexEntity>of(StellarVortexEntity::new, MobCategory.MISC)
                        .sized(1.0F, 1.0F)
                        .clientTrackingRange(8)
                        .updateInterval(20)
                        .build(ResourceKey.create(
                                Registries.ENTITY_TYPE,
                                ElementLib.id("stellar_vortex"))
                        ));
    }

    /** 示范内容关闭时整个注册器不挂到事件总线上。 */
    public static void register(IEventBus eventBus) {
        if (!ENABLED) {
            return;
        }
        ENTITIES.register(eventBus);
    }
}
