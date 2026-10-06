package com.linweiyun.elementlib.core.module.host;

import com.linweiyun.elementlib.core.attachment.ElibModuleAttachments;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import com.linweiyun.elementlib.core.module.ElibModuleData;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKind;
import com.linweiyun.elementlib.core.module.ElibModuleTargetKinds;
import com.linweiyun.elementlib.core.module.ElibModuleType;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 物品宿主：容器存在 ItemStack 的 DataComponent 上。
 *
 * <p>阶段 1 只读 —— ItemStack 会被复制和替换，写入契约等有明确需求再定。
 */
public final class ItemModuleHost implements ElibModuleHost {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.CORE);

    private final ItemStack stack;

    public ItemModuleHost(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public boolean isValid() {
        return !stack.isEmpty();
    }

    @Override
    public ElibModuleTargetKind kind() {
        return ElibModuleTargetKinds.ITEM;
    }

    @Override
    public String hostKey() {
        return "item:" + stack.getItem();
    }

    @Override
    @Nullable
    public ElibModuleContainer container() {
        return isValid() ? stack.get(ElibModuleAttachments.ITEM_MODULES.get()) : null;
    }

    @Override
    public void commit(ElibModuleContainer container) {
        LOGGER.warn("[ElementLib] 物品模块容器阶段 1 只读，忽略写入 host={}", hostKey());
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
        return get(type);
    }

    @Override
    public ItemStack itemStack() {
        return stack;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
