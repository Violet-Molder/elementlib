package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.ElementLib;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 宿主种类注册表 —— elementlib 内置三种，外部可继续注册。
 */
public final class ElibModuleTargetKinds {

    private static final Map<ResourceLocation, ElibModuleTargetKind> REGISTRY = new LinkedHashMap<>();

    /** 生物实体。 */
    public static final ElibModuleTargetKind ENTITY = register(ElementLib.id("entity"));
    /** 一个坐标上的方块。 */
    public static final ElibModuleTargetKind BLOCK = register(ElementLib.id("block"));
    /** 一个 ItemStack。 */
    public static final ElibModuleTargetKind ITEM = register(ElementLib.id("item"));

    private ElibModuleTargetKinds() {
    }

    /** 注册（或取回）一个宿主种类；同一个 id 重复注册返回同一个实例。 */
    public static synchronized ElibModuleTargetKind register(ResourceLocation id) {
        ElibModuleTargetKind existing = REGISTRY.get(id);
        if (existing != null) {
            return existing;
        }
        ElibModuleTargetKind kind = new ElibModuleTargetKind(id);
        REGISTRY.put(id, kind);
        return kind;
    }

    @Nullable
    public static ElibModuleTargetKind byId(@Nullable ResourceLocation id) {
        return id == null ? null : REGISTRY.get(id);
    }

    public static Collection<ElibModuleTargetKind> all() {
        return REGISTRY.values();
    }
}
