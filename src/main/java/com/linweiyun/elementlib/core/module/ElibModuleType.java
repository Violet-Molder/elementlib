package com.linweiyun.elementlib.core.module;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * <b>模块类型</b> —— 注册项类型。声明它支持哪些宿主、怎么存、怎么同步。
 *
 * <p>类型是代码注册的（不是数据包注册），用 {@link ElibModuleRegistry#register} 登记。
 */
public final class ElibModuleType<T extends ElibModuleData> {

    private final Identifier id;
    private final Class<T> dataClass;
    private final Set<Identifier> targetKinds;
    private final ElibModulePersistence persistence;
    private final ElibModuleSync sync;
    private final Supplier<T> factory;
    @Nullable
    private final Codec<T> codec;
    @Nullable
    private final StreamCodec<? super ByteBuf, T> streamCodec;

    private ElibModuleType(Builder<T> builder) {
        this.id = builder.id;
        this.dataClass = builder.dataClass;
        this.targetKinds = builder.targetKinds;
        this.persistence = builder.persistence;
        this.sync = builder.sync;
        this.factory = builder.factory;
        this.codec = builder.codec;
        this.streamCodec = builder.streamCodec;
    }

    public Identifier id() {
        return id;
    }

    public Class<T> dataClass() {
        return dataClass;
    }

    /** 这个模块能不能挂在某个宿主种类上。 */
    public boolean supports(ElibModuleTargetKind kind) {
        return targetKinds.contains(kind.id());
    }

    /** 运行期追加一个支持的宿主种类（例如 MineGenshin 给元素模块补上 character）。 */
    public ElibModuleType<T> support(ElibModuleTargetKind kind) {
        targetKinds.add(kind.id());
        return this;
    }

    public ElibModulePersistence persistence() {
        return persistence;
    }

    public ElibModuleSync sync() {
        return sync;
    }

    public boolean persistent() {
        return persistence == ElibModulePersistence.PERSISTENT;
    }

    /** 懒建一份新数据。 */
    public T create() {
        return factory.get();
    }

    @Nullable
    public Codec<T> codec() {
        return codec;
    }

    @Nullable
    public StreamCodec<? super ByteBuf, T> streamCodec() {
        return streamCodec;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    public static <T extends ElibModuleData> Builder<T> builder(Identifier id, Class<T> dataClass) {
        return new Builder<>(id, dataClass);
    }

    public static final class Builder<T extends ElibModuleData> {
        private final Identifier id;
        private final Class<T> dataClass;
        private final Set<Identifier> targetKinds = new LinkedHashSet<>();
        private ElibModulePersistence persistence = ElibModulePersistence.PERSISTENT;
        private ElibModuleSync sync = ElibModuleSync.NONE;
        private Supplier<T> factory;
        @Nullable
        private Codec<T> codec;
        @Nullable
        private StreamCodec<? super ByteBuf, T> streamCodec;

        private Builder(Identifier id, Class<T> dataClass) {
            this.id = id;
            this.dataClass = dataClass;
        }

        public Builder<T> supports(ElibModuleTargetKind... kinds) {
            for (ElibModuleTargetKind kind : kinds) {
                targetKinds.add(kind.id());
            }
            return this;
        }

        public Builder<T> persistent() {
            this.persistence = ElibModulePersistence.PERSISTENT;
            return this;
        }

        public Builder<T> transientPerLoad() {
            this.persistence = ElibModulePersistence.TRANSIENT_PER_LOAD;
            return this;
        }

        public Builder<T> sync(ElibModuleSync sync) {
            this.sync = sync;
            return this;
        }

        public Builder<T> factory(Supplier<T> factory) {
            this.factory = factory;
            return this;
        }

        public Builder<T> codec(@Nullable Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> streamCodec(@Nullable StreamCodec<? super ByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public ElibModuleType<T> build() {
            if (factory == null) {
                throw new IllegalStateException("模块 " + id + " 没有 factory");
            }
            return new ElibModuleType<>(this);
        }
    }
}
