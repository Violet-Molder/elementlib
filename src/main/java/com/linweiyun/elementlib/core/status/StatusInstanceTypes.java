package com.linweiyun.elementlib.core.status;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * StatusInstance 子类的类型注册表工具类。
 * <p>
 * 类型注册在 {@link com.linweiyun.elementlib.core.system.registry.register.ModStatusInstanceTypes}，
 * 落到 {@link ModRegistries#STATUS_INSTANCE_TYPE_REGISTRY}；序列化按 typeId 查找构造器重建子类实例。
 */
public final class StatusInstanceTypes {
    private static final Logger LOGGER = ModLog.getLogger(LogGroup.CORE);

    private StatusInstanceTypes() {}

    /**
     * 根据 typeId 创建实例：先在注册表里查构造器，查不到则返回一个立即过期的占位实例。
     *
     * @param typeId 类型标识字符串（如 "elemental_attachment"）
     */
    public static StatusInstance create(String typeId) {
        Identifier id = typeId.contains(":") ? Identifier.parse(typeId) : ElementLib.id(typeId);
        StatusInstanceType<?> type = ModRegistries.STATUS_INSTANCE_TYPE_REGISTRY.getOptional(id).orElse(null);
        if (type != null && type.constructor() != null) {
            return type.constructor().get();
        }
        LOGGER.warn("[StatusInstanceTypes] 未找到 typeId='{}' (id={}) 的注册条目，返回占位实例", typeId, id);
        return new StatusInstance() {
            @Override
            public void tick() {}

            @Override
            public boolean isFinished() {
                return true;
            }

            @Override
            public StatusInstance copy() {
                return this;
            }
        };
    }
}
