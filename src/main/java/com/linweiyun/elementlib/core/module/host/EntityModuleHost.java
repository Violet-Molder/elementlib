package com.linweiyun.elementlib.core.module.host;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.ElibModuleAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import com.linweiyun.elementlib.core.module.ElibModuleData;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKind;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKinds;
import com.linweiyun.elementlib.core.module.ElibModuleType;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>实体宿主</b> —— 模块容器存在实体附件 {@code elementlib:modules} 上。
 *
 * <p>旧存档里的 {@code elementlib:status_container}（元素容器）在第一次访问时
 * 被包成 element 模块条目搬到新容器里；旧键在 26.2.0.5 之前只读不写。
 */
public final class EntityModuleHost implements ElibModuleHost {

    private final LivingEntity entity;

    public EntityModuleHost(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public ElibModuleTargetKind kind() {
        return ElibModuleTargetKinds.ENTITY;
    }

    @Override
    public String hostKey() {
        return "entity:" + entity.getUUID();
    }

    @Override
    @Nullable
    public ElibModuleContainer container() {
        if (!isValid()) {
            return null;
        }
        // 双读单写：旧键有、新键没有 -> 迁移一次
        if (!entity.hasData(ElibModuleAttachments.MODULES.get())
                && entity.hasData(ElementalAttachments.CONTAINER.get())) {
            StatusContainer legacy = entity.getData(ElementalAttachments.CONTAINER);
            ElibModuleContainer migrated = new ElibModuleContainer();
            if (legacy != null) {
                migrated.put(legacy);
            }
            entity.setData(ElibModuleAttachments.MODULES.get(), migrated);
            return migrated;
        }
        return entity.getData(ElibModuleAttachments.MODULES);
    }

    @Override
    public void commit(ElibModuleContainer container) {
        entity.setData(ElibModuleAttachments.MODULES.get(), container);
    }

    @Override
    @Nullable
    public <T extends ElibModuleData> T get(ElibModuleType<T> type) {
        ElibModuleContainer container = container();
        return container == null ? null : container.get(type);
    }

    @Override
    @Nullable
    public <T extends ElibModuleData> T ensure(ElibModuleType<T> type) {
        if (!type.supports(kind())) {
            return null;
        }
        ElibModuleContainer container = container();
        return container == null ? null : container.ensure(type);
    }

    @Override
    public LivingEntity entity() {
        return entity;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
