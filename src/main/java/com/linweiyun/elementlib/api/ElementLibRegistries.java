package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册表句柄：元素 / 反应 / 反应类型三个 {@link DeferredRegister} 与它们的 {@link ResourceKey}。
 */
public final class ElementLibRegistries {

    private ElementLibRegistries() {
    }

    /** 元素注册表的 key（{@code elementlib:elements}）。 */
    public static final ResourceKey<Registry<GenshinElement>> ELEMENT_REGISTRY_KEY =
            ModRegistries.ELEMENT_REGISTRY_KEY;

    /** 反应注册表的 key（{@code elementlib:elemental_reactions}）。 */
    public static final ResourceKey<Registry<ElementalReaction>> REACTION_REGISTRY_KEY =
            ModRegistries.REACTION_REGISTRY_KEY;

    /** 反应类型注册表的 key（{@code elementlib:reaction_types}）。 */
    public static final ResourceKey<Registry<ElementalReactionType>> REACTION_TYPE_REGISTRY_KEY =
            ModRegistries.REACTION_TYPE_REGISTRY_KEY;

    /** 元素注册入口。 */
    public static final DeferredRegister<GenshinElement> ELEMENTS = ModRegistries.ELEMENTS;

    /** 反应注册入口。 */
    public static final DeferredRegister<ElementalReaction> REACTIONS = ModRegistries.ELEMENTAL_REACTIONS;

    /** 反应类型注册入口。 */
    public static final DeferredRegister<ElementalReactionType> REACTION_TYPES = ModRegistries.REACTION_TYPES;
}
