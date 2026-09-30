package com.linweiyun.elementlib.core.system.registry;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstanceType;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * elementlib 的三个自定义注册表：元素、元素反应、反应类型。
 *
 * <p>注册表对象在 {@link NewRegistryEvent} 里创建，{@code DeferredRegister} 由各自的持有类注册到事件总线。
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public class ModRegistries {

    public static final ResourceKey<Registry<ElementalReaction>> REACTION_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "elemental_reactions"));

    public static final ResourceKey<Registry<StatusInstanceType<?>>> STATUS_INSTANCE_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "status_instance_types"));

    public static final ResourceKey<Registry<GenshinElement>> ELEMENT_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "elements"));

    public static final ResourceKey<Registry<ElementalReactionType>> REACTION_TYPE_REGISTRY_KEY =
            ResourceKey.createRegistryKey(
                    Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "reaction_types"));

    // ======== 注册表 ========
    public static final Registry<ElementalReaction> ELEMENTAL_REACTIONS_REGISTRY =
            new RegistryBuilder<>(REACTION_REGISTRY_KEY)
                    .sync(true)
                    .maxId(64)
                    .create();

    public static final Registry<StatusInstanceType<?>> STATUS_INSTANCE_TYPE_REGISTRY =
            new RegistryBuilder<>(STATUS_INSTANCE_TYPE_REGISTRY_KEY)
                    .sync(true)
                    .maxId(256)
                    .create();

    public static final Registry<GenshinElement> ELEMENT_REGISTRY =
            new RegistryBuilder<>(ELEMENT_REGISTRY_KEY)
                    .sync(true)
                    .maxId(32)
                    .create();

    public static final Registry<ElementalReactionType> REACTION_TYPE_REGISTRY =
            new RegistryBuilder<>(REACTION_TYPE_REGISTRY_KEY)
                    .sync(true)
                    .maxId(64)
                    .create();

    public static final DeferredRegister<ElementalReaction> ELEMENTAL_REACTIONS =
            DeferredRegister.create(ELEMENTAL_REACTIONS_REGISTRY, ElementLib.MOD_ID);

    /** {@link #ELEMENTAL_REACTIONS} 的别名。 */
    public static final DeferredRegister<ElementalReaction> REACTIONS = ELEMENTAL_REACTIONS;

    public static final DeferredRegister<StatusInstanceType<?>> STATUS_INSTANCE_TYPES =
            DeferredRegister.create(STATUS_INSTANCE_TYPE_REGISTRY, ElementLib.MOD_ID);

    public static final DeferredRegister<GenshinElement> ELEMENTS =
            DeferredRegister.create(ELEMENT_REGISTRY, ElementLib.MOD_ID);

    public static final DeferredRegister<ElementalReactionType> REACTION_TYPES =
            DeferredRegister.create(REACTION_TYPE_REGISTRY, ElementLib.MOD_ID);

    @SubscribeEvent
    public static void registerRegistries(NewRegistryEvent event) {
        event.register(ELEMENT_REGISTRY);
        event.register(ELEMENTAL_REACTIONS_REGISTRY);
        event.register(STATUS_INSTANCE_TYPE_REGISTRY);
        event.register(REACTION_TYPE_REGISTRY);
    }
}
