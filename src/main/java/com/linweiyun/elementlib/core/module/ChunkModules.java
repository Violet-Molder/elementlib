package com.linweiyun.elementlib.core.module;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.utils.PersistedParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块级模块存储：持久区（写存档）+ 瞬态区（卸载即丢）+ 派生状态。
 */
public class ChunkModules implements IPersistedSerializable {

    public static final Codec<ChunkModules> CODEC = PersistedParser.createCodec(ChunkModules::new);

    private Map<Long, ElibModuleContainer> persistent = new HashMap<>();
    private final Map<Long, ElibModuleContainer> transientModules = new HashMap<>();
    private final Map<Long, Long> lastDecayTicks = new HashMap<>();

    public ChunkModules() {
    }

    public Map<Long, ElibModuleContainer> getPersistent() {
        return persistent;
    }

    @Nullable
    public ElibModuleContainer getPersistent(long key) {
        return persistent.get(key);
    }

    public void putPersistent(long key, ElibModuleContainer container) {
        persistent.put(key, container);
    }

    public boolean removePersistent(long key) {
        return persistent.remove(key) != null;
    }

    @Nullable
    public ElibModuleContainer getTransient(long key) {
        return transientModules.get(key);
    }

    public ElibModuleContainer transientAt(long key) {
        return transientModules.computeIfAbsent(key, k -> new ElibModuleContainer());
    }

    public void removeTransient(long key) {
        transientModules.remove(key);
    }

    @Nullable
    public Long getLastDecayTick(long key) {
        return lastDecayTicks.get(key);
    }

    public void putLastDecayTick(long key, long tick) {
        lastDecayTicks.put(key, tick);
    }

    public void removeLastDecayTick(long key) {
        lastDecayTicks.remove(key);
    }

    @Override
    public void serialize(@NotNull ValueOutput output) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Map.Entry<Long, ElibModuleContainer> entry : persistent.entrySet()) {
            Tag payload = ElibModuleContainer.CODEC
                    .encodeStart(NbtOps.INSTANCE, entry.getValue())
                    .result().orElse(null);
            if (payload == null) {
                continue;
            }
            CompoundTag item = new CompoundTag();
            item.put("pos", LongTag.valueOf(entry.getKey()));
            item.put("data", payload);
            list.add(item);
        }
        root.put("entries", list);
        output.store(root);
    }

    @Override
    public void deserialize(@NotNull ValueInput input) {
        CompoundTag root = input.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC))
                .orElse(new CompoundTag());
        Map<Long, ElibModuleContainer> loaded = new HashMap<>();
        ListTag list = root.getList("entries").orElse(new ListTag());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompoundOrEmpty(i);
            Tag payload = item.get("data");
            if (payload == null) {
                continue;
            }
            ElibModuleContainer container = ElibModuleContainer.CODEC
                    .parse(NbtOps.INSTANCE, payload)
                    .result().orElse(null);
            if (container != null) {
                loaded.put(item.getLong("pos").orElse(0L), container);
            }
        }
        this.persistent = loaded;
    }
}
