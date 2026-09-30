package com.linweiyun.elementlib.content.items;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 元素法杖：右键生物附着当前元素；潜行右键切换元素；非潜行右键空气给自己附着。
 */
public class ElementWandItem extends Item {

    /** 同源附着匹配用的来源标识。 */
    public static final String SOURCE_KEY = "element_wand";

    public ElementWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player,
                                          @NotNull InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            cycle(level, player, stack);
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        attach(player, stack, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack itemStack, @NotNull Player player,
                                                           @NotNull LivingEntity target,
                                                           @NotNull InteractionHand type) {
        if (player.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        attach(player, itemStack, target);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        Identifier id = selectedId(stack);
        if (id == null) {
            builder.accept(Component.translatable("item.elementlib.element_wand.no_elements"));
            return;
        }
        builder.accept(Component.translatable("item.elementlib.element_wand.selected", displayNameOf(id)));
        builder.accept(Component.translatable("item.elementlib.element_wand.tooltip.attach")
                .withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.elementlib.element_wand.tooltip.cycle")
                .withStyle(ChatFormatting.GRAY));
    }

    // ==================== 附着 ====================

    private static void attach(Player player, ItemStack stack, LivingEntity target) {
        Identifier id = selectedId(stack);
        GenshinElement element = id == null ? null : ModRegistries.ELEMENT_REGISTRY.getValue(id);
        if (element == null) {
            player.sendSystemMessage(Component.translatable("item.elementlib.element_wand.no_elements"));
            return;
        }
        AttachResult result = ElementalAttachmentHelper.attach(EntityHost.of(target), element,
                AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK,
                SOURCE_KEY, player.level().getGameTime());
        String outcome = result.reacted() ? "reacted" : result.attached() ? "attached" : "rejected";
        player.sendSystemMessage(Component.literal("[ElementLib] ")
                .append(Component.translatable(element.getTranslationKey()))
                .append(Component.literal(" → " + target.getName().getString() + ": " + outcome)));
    }

    // ==================== 元素选择 ====================

    private static void cycle(Level level, Player player, ItemStack stack) {
        List<Identifier> ids = elementIds();
        if (ids.isEmpty()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("item.elementlib.element_wand.no_elements"));
            }
            return;
        }
        Identifier current = stack.get(ModItems.WAND_ELEMENT);
        int index = current == null ? -1 : ids.indexOf(current);
        Identifier next = ids.get((index + 1) % ids.size());
        DataComponentType<Identifier> type = ModItems.wandElementType();
        if (type != null) {
            stack.set(type, next);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "item.elementlib.element_wand.selected", displayNameOf(next)));
        }
    }

    /** 当前选中的元素 id；未选中或已失效时退回列表第一个，列表为空返回 {@code null}。 */
    @Nullable
    private static Identifier selectedId(ItemStack stack) {
        List<Identifier> ids = elementIds();
        if (ids.isEmpty()) {
            return null;
        }
        Identifier id = stack.get(ModItems.WAND_ELEMENT);
        if (id == null || ModRegistries.ELEMENT_REGISTRY.getValue(id) == null) {
            return ids.get(0);
        }
        return id;
    }

    /**
     * 可切换的元素 id，按名称排序保证切换顺序稳定。
     *
     * <p>类元素（冻/激/燃/木）、效果载体（寒）不能由外部直接附着，物理没有附着含义，
     * 留在切换表里只是徒占位置。
     */
    private static List<Identifier> elementIds() {
        List<Identifier> ids = new ArrayList<>();
        for (Identifier id : ModRegistries.ELEMENT_REGISTRY.keySet()) {
            GenshinElement element = ModRegistries.ELEMENT_REGISTRY.getValue(id);
            if (element == null) continue;
            if (!element.allowsDirectAttachment() || element.isEffectCarrier()) continue;
            if (ModElements.is(element, ModElements.FYSIKOS)) continue;
            ids.add(id);
        }
        Collections.sort(ids);
        return ids;
    }

    private static Component displayNameOf(Identifier id) {
        GenshinElement element = ModRegistries.ELEMENT_REGISTRY.getValue(id);
        return element == null ? Component.literal(id.toString())
                : Component.translatable(element.getTranslationKey());
    }
}
