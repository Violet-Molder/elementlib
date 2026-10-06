package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.core.module.host.EntityModuleHost;
import com.linweiyun.elementlib.core.module.host.BlockModuleHost;
import com.linweiyun.elementlib.core.module.host.ItemModuleHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * 宿主工厂 —— 统一入口。
 */
public final class ElibModuleHosts {

    private interface CarrierFactory {
        Class<?> carrierType();

        ElibModuleHost create(Object carrier);
    }

    private static final List<CarrierFactory> CARRIERS = new CopyOnWriteArrayList<>();

    private ElibModuleHosts() {
    }

    /** 外部载体的宿主工厂（MineGenshin 用它注册 PGCharacter）。 */
    public static <C> void registerCarrier(Class<C> carrierType, Function<C, ElibModuleHost> factory) {
        CARRIERS.add(new CarrierFactory() {
            @Override
            public Class<?> carrierType() {
                return carrierType;
            }

            @Override
            @SuppressWarnings("unchecked")
            public ElibModuleHost create(Object carrier) {
                return factory.apply((C) carrier);
            }
        });
    }

    @Nullable
    public static ElibModuleHost ofCarrier(@Nullable Object carrier) {
        if (carrier == null) {
            return null;
        }
        for (CarrierFactory factory : CARRIERS) {
            if (factory.carrierType().isInstance(carrier)) {
                return factory.create(carrier);
            }
        }
        return null;
    }

    @Nullable
    public static ElibModuleHost of(@Nullable Entity entity) {
        return entity instanceof LivingEntity living ? new EntityModuleHost(living) : null;
    }

    @Nullable
    public static ElibModuleHost of(@Nullable ServerLevel level, @Nullable BlockPos pos) {
        return (level == null || pos == null) ? null : new BlockModuleHost(level, pos);
    }

    @Nullable
    public static ElibModuleHost of(@Nullable ItemStack stack, @Nullable Level level) {
        return (stack == null || stack.isEmpty()) ? null : new ItemModuleHost(stack);
    }
}
