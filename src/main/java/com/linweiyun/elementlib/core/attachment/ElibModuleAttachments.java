package com.linweiyun.elementlib.core.attachment;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.module.ChunkModules;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 模块容器的附件注册。
 *
 * <p>{@code elementlib:modules} 是实体上的通用模块容器；元素是其中一个模块。
 * 旧的 {@code elementlib:status_container} 仍然注册着，只用于兼容读取。
 */
public final class ElibModuleAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementLib.MOD_ID);

    /** 物品宿主的模块容器挂在 DataComponent 上。 */
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementLib.MOD_ID);

    /** 实体身上的模块容器（可序列化 + 同步）。 */
    public static final Supplier<AttachmentType<ElibModuleContainer>> MODULES =
            ATTACHMENTS.register("modules",
                    () -> AttachmentType.serializable(ElibModuleContainer::new)
                            .sync(ElibModuleContainer.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    /** 区块级模块存储：持久区 + 瞬态区。 */
    public static final Supplier<AttachmentType<ChunkModules>> CHUNK_MODULES =
            ATTACHMENTS.register("chunk_modules",
                    () -> AttachmentType.serializable(ChunkModules::new).build());

    /** ItemStack 上的模块容器；阶段 1 只读。 */
    public static final Supplier<DataComponentType<ElibModuleContainer>> ITEM_MODULES =
            DATA_COMPONENTS.registerComponentType("item_modules",
                    builder -> builder.persistent(ElibModuleContainer.CODEC)
                            .networkSynchronized(ElibModuleContainer.STREAM_CODEC));

    private ElibModuleAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
    }
}
