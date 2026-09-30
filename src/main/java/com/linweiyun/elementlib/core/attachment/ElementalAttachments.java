package com.linweiyun.elementlib.core.attachment;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.system.about.block.ChunkBlockElements;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterManager;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * 元素体系的附件注册：生物容器、区块方块容器、计时计数器。
 */
public final class ElementalAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementLib.MOD_ID);

    /** 生物身上的元素状态容器（可序列化 + 同步）。 */
    public static final Supplier<AttachmentType<StatusContainer>> CONTAINER =
            ATTACHMENTS.register("status_container",
                    () -> AttachmentType.serializable(StatusContainer::new)
                            .sync(StatusContainer.STREAM_CODEC)
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

    /** 便捷取容器。 */
    public static StatusContainer container(LivingEntity entity) {
        return entity.getData(CONTAINER);
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