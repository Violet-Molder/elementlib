package com.linweiyun.elementlib.core.attachment;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.module.ElibModuleContainer;
import com.linweiyun.elementlib.core.module.ElibModuleData;
import com.linweiyun.elementlib.core.module.ElibModuleHost;
import com.linweiyun.elementlib.core.module.ElibModuleHosts;
import com.linweiyun.elementlib.core.module.ElibModuleTypes;
import com.linweiyun.elementlib.core.system.about.block.ChunkBlockElements;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 元素体系的附件注册：生物容器、区块方块容器、计时计数器。
 */
public final class ElementalAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementLib.MOD_ID);

    /**
     * 旧版生物元素容器（可序列化 + 同步）。
     *
     * <p><b>兼容专用</b>：26.2.0.4 起元素容器搬进 {@code elementlib:modules}
     * （见 {@link ElibModuleAttachments#MODULES}），这个附件只用于读旧存档，
     * 计划在 26.2.0.5 删除。
     */
    @Deprecated
    public static final Supplier<AttachmentType<StatusContainer>> CONTAINER =
            ATTACHMENTS.register("status_container",
                    () -> AttachmentType.serializable(StatusContainer::new)
                            .sync(StatusContainer.STREAM_CODEC)
                            .copyOnDeath()
                            .build());

    /** 区块上的方块元素容器表（只服务端用）。 */
    public static final Supplier<AttachmentType<ChunkBlockElements>> CHUNK_ELEMENTS =
            ATTACHMENTS.register("chunk_elements",
                    () -> AttachmentType.builder(ChunkBlockElements::new)
                            .serialize(ChunkBlockElements.CODEC)
                            .build());

    /** 计时计数器管理器（每实体一份，不落存档）。 */
    public static final Supplier<AttachmentType<DecayCounterManager>> DECAY_COUNTER =
            ATTACHMENTS.register("decay_counter",
                    () -> AttachmentType.<DecayCounterManager>builder(() -> null)
                            .build());

    private ElementalAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    /**
     * 取元素容器（可写）。走模块入口：新存档存在 {@code elementlib:modules} 里，
     * 第一次访问旧存档时把旧附件迁移进来。
     */
    public static StatusContainer container(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        ElibModuleHost host = ElibModuleHosts.of(entity);
        StatusContainer container = host == null ? null : host.ensure(ElibModuleTypes.ELEMENT);
        if (container != null) {
            return container;
        }
        // 兼容旧语义：无效宿主（已死亡/移除）也返回一份可读容器
        return entity.getData(ElibModuleAttachments.MODULES).ensure(ElibModuleTypes.ELEMENT);
    }

    /**
     * 只读取元素容器：没有就返回 {@code null}。
     *
     * <p>客户端渲染、只读扫描用它，避免在客户端凭空建容器。
     */
    @Nullable
    public static StatusContainer peekContainer(LivingEntity entity) {
        if (entity == null || !entity.hasData(ElibModuleAttachments.MODULES.get())) {
            return null;
        }
        ResourceLocation elementId = ElibModuleTypes.ELEMENT.id();
        ElibModuleData data = entity.getData(ElibModuleAttachments.MODULES).getRaw(elementId);
        return data instanceof StatusContainer status ? status : null;
    }

    /** 把元素容器的改动落回模块容器（触发同步/落盘）。 */
    public static void commit(LivingEntity entity, StatusContainer container) {
        ElibModuleHost host = ElibModuleHosts.of(entity);
        if (host == null) {
            return;
        }
        ElibModuleContainer modules = host.container();
        if (modules == null) {
            return;
        }
        modules.put(container);
        host.commit(modules);
    }

    /** 便捷取计时计数器管理器：没有就现建一个并 {@code setData}（computeIfAbsent 语义）。 */
    public static DecayCounterManager decayCounter(LivingEntity entity) {
        DecayCounterManager manager = entity.getData(DECAY_COUNTER);
        if (manager == null) {
            manager = new DecayCounterManager(entity);
            entity.setData(DECAY_COUNTER, manager);
        }
        return manager;
    }
}