package com.linweiyun.elementlib.core.attachment;

import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.status.StatusInstanceTypes;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.FrozenDecayState;
import com.linweiyun.elementlib.core.system.reaction.ElectroChargedTickState;
import com.lowdragmc.lowdraglib2.LDLib2;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
                    && ElementRoles.is(ea.getElement(), ElementRoles.FROZEN)) {
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

    // ========== 多态序列化（NBT 路径：ValueOutput/ValueInput） ==========

    @Override
    public void serialize(@NotNull ValueOutput output) {
        beforeSerialize();
        try {
            CompoundTag root = new CompoundTag();
            HolderLookup.Provider provider = Platform.getFrozenRegistry();

            CompoundTag frozenTag = PersistedParser.serializeNBT(frozenDecayState, provider);
            root.put("frozen_decay_state", frozenTag);

            CompoundTag ecTickTag = PersistedParser.serializeNBT(electroChargedTickState, provider);
            root.put("electro_charged_tick_state", ecTickTag);

            ListTag list = new ListTag();
            for (StatusInstance inst : instances) {
                try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    var elemOut = TagValueOutput.createWithContext(reporter, provider);
                    inst.serialize(elemOut);
                    list.add(elemOut.buildResult());
                }
            }
            root.put("statuses", list);

            output.store(root);
        } finally {
            afterSerialize();
        }
    }

    @Override
    public void deserialize(@NotNull ValueInput input) {
        beforeDeserialize();
        try {
            CompoundTag root = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC))
                    .orElse(new CompoundTag());
            HolderLookup.Provider provider = input.lookup();

            if (root.contains("frozen_decay_state")) {
                frozenDecayState = new FrozenDecayState();
                PersistedParser.deserializeNBT(
                        root.getCompound("frozen_decay_state").orElse(new CompoundTag()),
                        frozenDecayState, provider);
            }

            if (root.contains("electro_charged_tick_state")) {
                electroChargedTickState = new ElectroChargedTickState();
                PersistedParser.deserializeNBT(
                        root.getCompound("electro_charged_tick_state").orElse(new CompoundTag()),
                        electroChargedTickState, provider);
            }

            instances = new ArrayList<>();
            if (root.contains("statuses")) {
                ListTag list = root.getList("statuses").orElse(new ListTag());
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag instTag = list.getCompoundOrEmpty(i);
                    String typeId = instTag.getString("type_id").orElse("");
                    StatusInstance inst = StatusInstanceTypes.create(typeId);
                    try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                        inst.deserialize(TagValueInput.create(reporter, provider, instTag));
                    }
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
            CompoundTag root = new CompoundTag();
            HolderLookup.Provider provider = Platform.getFrozenRegistry();

            CompoundTag frozenTag = PersistedParser.serializeNBT(frozenDecayState, provider);
            root.put("frozen_decay_state", frozenTag);

            CompoundTag ecTickTag = PersistedParser.serializeNBT(electroChargedTickState, provider);
            root.put("electro_charged_tick_state", ecTickTag);

            ListTag list = new ListTag();
            for (StatusInstance inst : instances) {
                try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                    var elemOut = TagValueOutput.createWithContext(reporter, provider);
                    inst.serialize(elemOut);
                    list.add(elemOut.buildResult());
                }
            }
            root.put("statuses", list);

            new FriendlyByteBuf(buf).writeNbt(root);
        } finally {
            afterSerialize();
        }
    }

    @Override
    public void readFromBuff(ByteBuf buf) {
        beforeDeserialize();
        try {
            CompoundTag root = new FriendlyByteBuf(buf).readNbt();
            if (root == null) root = new CompoundTag();
            HolderLookup.Provider provider = Platform.getFrozenRegistry();

            if (root.contains("frozen_decay_state")) {
                frozenDecayState = new FrozenDecayState();
                PersistedParser.deserializeNBT(
                        root.getCompound("frozen_decay_state").orElse(new CompoundTag()),
                        frozenDecayState, provider);
            }

            if (root.contains("electro_charged_tick_state")) {
                electroChargedTickState = new ElectroChargedTickState();
                PersistedParser.deserializeNBT(
                        root.getCompound("electro_charged_tick_state").orElse(new CompoundTag()),
                        electroChargedTickState, provider);
            }

            instances = new ArrayList<>();
            if (root.contains("statuses")) {
                ListTag list = root.getList("statuses").orElse(new ListTag());
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag instTag = list.getCompoundOrEmpty(i);
                    String typeId = instTag.getString("type_id").orElse("");
                    StatusInstance inst = StatusInstanceTypes.create(typeId);
                    try (var reporter = new ProblemReporter.ScopedCollector(LDLib2.LOGGER)) {
                        inst.deserialize(TagValueInput.create(reporter, provider, instTag));
                    }
                    add(inst);
                }
            }
        } finally {
            afterDeserialize();
        }
    }
}
