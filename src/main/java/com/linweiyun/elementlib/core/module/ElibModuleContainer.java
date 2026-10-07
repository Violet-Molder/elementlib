package com.linweiyun.elementlib.core.module;

import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>模块容器</b> —— 一个宿主身上的全部模块数据。
 *
 * <p>形状与已有的 {@code StatusContainer} 一致：容器里装若干「带注册类型的数据」。
 * 元素是其中的第一个模块（{@link ElibModuleTypes#ELEMENT}），韧性由 MineGenshin 注册。
 */
public class ElibModuleContainer implements IPersistedSerializable {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.CORE);

    public static final Codec<ElibModuleContainer> CODEC =
            PersistedParser.createCodec(ElibModuleContainer::new);
    public static final StreamCodec<ByteBuf, ElibModuleContainer> STREAM_CODEC =
            PersistedParser.createStreamCodec(ElibModuleContainer::new);

    private Map<ResourceLocation, ElibModuleData> modules = new LinkedHashMap<>();

    public ElibModuleContainer() {
    }

    public Collection<ElibModuleData> all() {
        return modules.values();
    }

    public boolean isEmpty() {
        return modules.isEmpty();
    }

    @Nullable
    public ElibModuleData getRaw(@Nullable ResourceLocation id) {
        return id == null ? null : modules.get(id);
    }

    @Nullable
    public <T extends ElibModuleData> T get(ElibModuleType<T> type) {
        ElibModuleData data = modules.get(type.id());
        return type.dataClass().isInstance(data) ? type.dataClass().cast(data) : null;
    }

    /** 读取；没有就懒建一份并放进容器。 */
    public <T extends ElibModuleData> T ensure(ElibModuleType<T> type) {
        T existing = get(type);
        if (existing != null) {
            return existing;
        }
        T created = type.create();
        modules.put(type.id(), created);
        return created;
    }

    public void put(ElibModuleData data) {
        modules.put(data.typeId(), data);
    }

    public boolean has(ElibModuleType<?> type) {
        return modules.containsKey(type.id());
    }

    public boolean remove(ElibModuleType<?> type) {
        return modules.remove(type.id()) != null;
    }

    public void clear() {
        modules.clear();
    }

    /** 拷贝一份容器（宿主复制时用）。 */
    public ElibModuleContainer copy() {
        ElibModuleContainer copy = new ElibModuleContainer();
        copy.modules.putAll(this.modules);
        return copy;
    }

    // ==================== 存档序列化（NBT） ====================

    @Override
    public CompoundTag serializeNBT(@NotNull HolderLookup.Provider provider) {
        return toTag();
    }

    @Override
    public void deserializeNBT(@NotNull HolderLookup.Provider provider, @NotNull CompoundTag root) {
        fromTag(root);
    }

    private CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (ElibModuleData data : modules.values()) {
            ElibModuleType<?> type = data.type();
            Codec<?> codec = type.codec();
            if (codec == null) {
                continue;
            }
            Tag payload = encode(codec, data);
            if (payload == null) {
                LOGGER.warn("[ElementLib] 模块 {} 序列化失败，跳过", type.id());
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.put("type", StringTag.valueOf(type.id().toString()));
            entry.put("data", payload);
            list.add(entry);
        }
        root.put("modules", list);
        return root;
    }

    private void fromTag(CompoundTag root) {
        Map<ResourceLocation, ElibModuleData> loaded = new LinkedHashMap<>();
        ListTag list = root.getList("modules", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            String rawId = entry.getString("type");
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            ElibModuleType<?> type = ElibModuleRegistry.byId(id);
            if (type == null || type.codec() == null) {
                LOGGER.warn("[ElementLib] 存档里的模块类型 {} 未注册，跳过", rawId);
                continue;
            }
            Tag payload = entry.get("data");
            if (payload == null) {
                continue;
            }
            ElibModuleData data = decode(type.codec(), payload);
            if (data == null) {
                LOGGER.warn("[ElementLib] 模块 {} 反序列化失败，跳过", rawId);
                continue;
            }
            loaded.put(type.id(), data);
        }
        this.modules = loaded;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private static Tag encode(Codec codec, Object data) {
        return (Tag) codec.encodeStart(NbtOps.INSTANCE, data).result().orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private static ElibModuleData decode(Codec codec, Tag payload) {
        return (ElibModuleData) codec.parse(NbtOps.INSTANCE, payload).result().orElse(null);
    }

    // ==================== 网络同步（ByteBuf） ====================

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void writeToBuff(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        List<ElibModuleData> writable = new ArrayList<>();
        for (ElibModuleData data : modules.values()) {
            if (data.type().streamCodec() != null) {
                writable.add(data);
            }
        }
        friendly.writeVarInt(writable.size());
        for (ElibModuleData data : writable) {
            ElibModuleType type = data.type();
            friendly.writeResourceLocation(type.id());
            ((StreamCodec) type.streamCodec()).encode(friendly, data);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void readFromBuff(ByteBuf buf) {
        FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
        int size = friendly.readVarInt();
        Map<ResourceLocation, ElibModuleData> loaded = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation id = friendly.readResourceLocation();
            ElibModuleType<?> type = ElibModuleRegistry.byId(id);
            if (type == null || type.streamCodec() == null) {
                throw new IllegalStateException("收到未注册的模块类型：" + id);
            }
            ElibModuleData data = (ElibModuleData) ((StreamCodec) type.streamCodec()).decode(friendly);
            loaded.put(id, data);
        }
        this.modules = loaded;
    }
}
