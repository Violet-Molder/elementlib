package com.linweiyun.elementlib.core.module;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模块类型注册表 —— 代码注册，不走数据包。
 */
public final class ElibModuleRegistry {

    private static final Map<ResourceLocation, ElibModuleType<?>> TYPES = new LinkedHashMap<>();

    private ElibModuleRegistry() {
    }

    public static synchronized <T extends ElibModuleData> ElibModuleType<T> register(ElibModuleType<T> type) {
        ElibModuleType<?> previous = TYPES.putIfAbsent(type.id(), type);
        if (previous != null && previous != type) {
            throw new IllegalStateException("模块类型 id 冲突：" + type.id());
        }
        return type;
    }

    @Nullable
    public static ElibModuleType<?> byId(@Nullable ResourceLocation id) {
        return id == null ? null : TYPES.get(id);
    }

    public static Collection<ElibModuleType<?>> all() {
        return TYPES.values();
    }
}
