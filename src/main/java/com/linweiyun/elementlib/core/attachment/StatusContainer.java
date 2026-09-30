package com.linweiyun.elementlib.core.attachment;

import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.status.StatusInstanceTypes;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.FrozenDecayState;
import com.linweiyun.elementlib.core.system.reaction.ElectroChargedTickState;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 状态容器 —— 某个宿主身上所有 StatusInstance 的集合
 */
public class StatusContainer implements IPersistedSerializable {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.CORE);
    public final static Codec<StatusContainer> CODEC = PersistedParser.createCodec(StatusContainer::new);
    public final static StreamCodec<ByteBuf, StatusContainer> STREAM_CODEC = PersistedParser.createStreamCodec(StatusContainer::new);

    @Persisted(key = "statuses")
    private List<StatusInstance> instances = new ArrayList<>();

    @Persisted(key = "frozen_decay_state")
    private FrozenDecayState frozenDecayState = new FrozenDecayState();

    @Persisted(key = "electro_charged_tick_state")
    private ElectroChargedTickState electroChargedTickState = new ElectroChargedTickState();

    public StatusContainer() {
        this.instances = new ArrayList<>();
    }

    public static final StatusContainer EMPTY = new StatusContainer();

    // ========== 存取 ==========

    public void add(StatusInstance instance) {
        instances.add(instance);
        if (instance instanceof ElementalAttachmentInstance ea) {
            ea.setContainer(this);
        }
    }

    public StatusInstance find(Predicate<StatusInstance> matcher) {
        return instances.stream().filter(matcher).findFirst().orElse(null);
    }

    public boolean hasAlive(Predicate<StatusInstance> matcher) {
        return instances.stream().anyMatch(i -> !i.isFinished() && matcher.test(i));
    }

    public boolean removeFirst(Predicate<StatusInstance> matcher) {
        Iterator<StatusInstance> it = instances.iterator();
        while (it.hasNext()) {
            StatusInstance inst = it.next();
            if (matcher.test(inst)) {
                inst.onRemove();
                it.remove();
                return true;
            }
        }
        return false;
    }

    public void remove(StatusInstance instance) {
        instance.onRemove();
        instances.remove(instance);
    }

    public void clear() {
        for (StatusInstance inst : instances) {
            inst.onRemove();
        }
        instances.clear();
    }

    public List<StatusInstance> getAll() { return instances; }
    public boolean isEmpty() { return instances.isEmpty(); }

    // ========== Tick ==========

    public void tick() {
        boolean hadFrozenAlive = false;
        for (StatusInstance inst : instances) {
            if (!inst.isFinished()
                    && inst instanceof ElementalAttachmentInstance ea
                    && ModElements.is(ea.getElement(), ModElements.FROZEN)) {
                hadFrozenAlive = true;
                break;
            }
        }
        Iterator<StatusInstance> it = instances.iterator();
        while (it.hasNext()) {
            StatusInstance inst = it.next();
            inst.tick();
            if (inst.isFinished()) {
                inst.onRemove();
                it.remove();
            }
        }
        frozenDecayState.onTick(hadFrozenAlive);
        electroChargedTickState.setContainer(this);
        electroChargedTickState.onTick();
    }

    public FrozenDecayState getFrozenDecayState() {
        return frozenDecayState;
    }

    public ElectroChargedTickState getElectroChargedTickState() {
        return electroChargedTickState;
    }

    // ========== 拷贝 ==========

    public StatusContainer copy() {
        StatusContainer c = new StatusContainer();
        for (StatusInstance inst : instances) {
            c.add(inst.copy());
        }
        return c;
    }

    // ========== 多态序列化（NBT 路径） ==========

    @Override
    public CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        beforeSerialize();
        try {
            CompoundTag root = new CompoundTag();

            root.put("frozen_decay_state", PersistedParser.serializeNBT(frozenDecayState, provider));
            root.put("electro_charged_tick_state", PersistedParser.serializeNBT(electroChargedTickState, provider));

            ListTag list = new ListTag();
            for (StatusInstance inst : instances) {
                list.add(PersistedParser.serializeNBT(inst, provider));
            }
            root.put("statuses", list);
            return root;
        } finally {
            afterSerialize();
        }
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag root) {
        beforeDeserialize();
        try {
            if (root.contains("frozen_decay_state")) {
                frozenDecayState = new FrozenDecayState();
                PersistedParser.deserializeNBT(
                        root.getCompound("frozen_decay_state"),
                        frozenDecayState, provider);
            }

            if (root.contains("electro_charged_tick_state")) {
                electroChargedTickState = new ElectroChargedTickState();
                PersistedParser.deserializeNBT(
                        root.getCompound("electro_charged_tick_state"),
                        electroChargedTickState, provider);
            }

            instances = new ArrayList<>();
            if (root.contains("statuses")) {
                    ListTag list = root.getList("statuses", Tag.TAG_COMPOUND);
                    for (int i = 0; i < list.size(); i++) {
                        CompoundTag instTag = list.getCompound(i);
                        String typeId = instTag.getString("type_id");
                        StatusInstance inst = StatusInstanceTypes.create(typeId);
                        if (inst == null) {
                            continue;
                        }
                        PersistedParser.deserializeNBT(instTag, inst, provider);
                        add(inst);
                    }
                }
        } finally {
            afterDeserialize();
        }
    }

    // ========== 多态序列化（网络路径：ByteBuf） ==========

    @Override
    public void writeToBuff(ByteBuf buf) {
        beforeSerialize();
        try {
            HolderLookup.Provider provider = com.lowdragmc.lowdraglib2.Platform.getFrozenRegistry();
            new FriendlyByteBuf(buf).writeNbt(serializeNBT(provider));
        } finally {
            afterSerialize();
        }
    }

    @Override
    public void readFromBuff(ByteBuf buf) {
        beforeDeserialize();
        try {
            CompoundTag root = new FriendlyByteBuf(buf).readNbt();
            if (root == null) {
                root = new CompoundTag();
            }
            HolderLookup.Provider provider = com.lowdragmc.lowdraglib2.Platform.getFrozenRegistry();
            deserializeNBT(provider, root);
        } finally {
            afterDeserialize();
        }
    }
}