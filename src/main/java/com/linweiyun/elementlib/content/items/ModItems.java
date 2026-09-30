package com.linweiyun.elementlib.content.items;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.config.ElementLibConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 示范物品注册 —— {@code items.toml} 的 {@code register-demo-items} 关闭时不注册任何内容。
 */
public final class ModItems {

    /** 元素剑的攻击力与攻速基线。 */
    private static final float SWORD_ATTACK_DAMAGE = 3.0F;
    private static final float SWORD_ATTACK_SPEED = -2.4F;

    /** 原版剑攻速/攻速基准 modifier 的固定 id（与原版 SwordItem 一致）。 */
    private static final ResourceLocation BASE_ATTACK_DAMAGE_ID =
            ResourceLocation.withDefaultNamespace("generic.attack_damage");
    private static final ResourceLocation BASE_ATTACK_SPEED_ID =
            ResourceLocation.withDefaultNamespace("generic.attack_speed");

    /** 元素剑的基础属性：攻击伤害 + 攻速（等效原版铁剑）。 */
    private static final ItemAttributeModifiers SWORD_ATTRIBUTES =
            ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(BASE_ATTACK_DAMAGE_ID, SWORD_ATTACK_DAMAGE,
                                    AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED,
                            new AttributeModifier(BASE_ATTACK_SPEED_ID, SWORD_ATTACK_SPEED,
                                    AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.MAINHAND)
                    .build();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementLib.MOD_ID);

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementLib.MOD_ID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ElementLib.MOD_ID);

    /** 元素法杖当前选中的元素 id。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> WAND_ELEMENT =
            DATA_COMPONENTS.registerComponentType(
                    "wand_element",
                    builder -> builder
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ResourceLocation.STREAM_CODEC)
            );

    public static final DeferredItem<ElementWandItem> ELEMENT_WAND =
            ITEMS.registerItem("element_wand", ElementWandItem::new);

    // ======== 七把元素剑，item id = 元素 path + _sword ========

    public static final DeferredItem<ElementSwordItem> PYRO_SWORD = sword("pyro");
    public static final DeferredItem<ElementSwordItem> HYDRO_SWORD = sword("hydro");
    public static final DeferredItem<ElementSwordItem> ELECTRO_SWORD = sword("electro");
    public static final DeferredItem<ElementSwordItem> CYRO_SWORD = sword("cyro");
    public static final DeferredItem<ElementSwordItem> ANEMO_SWORD = sword("anemo");
    public static final DeferredItem<ElementSwordItem> DENDRO_SWORD = sword("dendro");
    public static final DeferredItem<ElementSwordItem> GEO_SWORD = sword("geo");

    /** 全部元素剑，供命令一次性发放。 */
    public static final List<DeferredItem<ElementSwordItem>> ELEMENT_SWORDS = List.of(
            PYRO_SWORD, HYDRO_SWORD, ELECTRO_SWORD, CYRO_SWORD,
            ANEMO_SWORD, DENDRO_SWORD, GEO_SWORD);

    /** 示范物品的物品组：法杖 + 七把元素剑。 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEMO_TAB =
            CREATIVE_TABS.register("demo_items", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.elementlib.demo_items"))
                    .icon(ModItems::tabIcon)
                    .displayItems((parameters, output) -> {
                        if (ELEMENT_WAND.isBound()) {
                            output.accept(new ItemStack(ELEMENT_WAND.get()));
                        }
                        for (DeferredItem<ElementSwordItem> sword : ELEMENT_SWORDS) {
                            if (sword.isBound()) {
                                output.accept(new ItemStack(sword.get()));
                            }
                        }
                    })
                    .build());

    private ModItems() {
    }

    /** 物品组图标：法杖未注册时退回第一把已注册的元素剑。 */
    private static ItemStack tabIcon() {
        if (ELEMENT_WAND.isBound()) {
            return new ItemStack(ELEMENT_WAND.get());
        }
        for (DeferredItem<ElementSwordItem> sword : ELEMENT_SWORDS) {
            if (sword.isBound()) {
                return new ItemStack(sword.get());
            }
        }
        return ItemStack.EMPTY;
    }

    private static DeferredItem<ElementSwordItem> sword(String elementPath) {
        return ITEMS.registerItem(
                elementPath + "_sword",
                props -> new ElementSwordItem(props, ElementLib.MOD_ID + ":" + elementPath),
                new Item.Properties().attributes(SWORD_ATTRIBUTES));
    }

    public static void register(IEventBus modEventBus) {
        if (!ElementLibConfig.demoItemsEnabled()) {
            return;
        }
        DATA_COMPONENTS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
    }

    /** 法杖元素数据组件类型；未注册时为 {@code null}。 */
    @Nullable
    public static DataComponentType<ResourceLocation> wandElementType() {
        return WAND_ELEMENT.isBound() ? WAND_ELEMENT.get() : null;
    }
}