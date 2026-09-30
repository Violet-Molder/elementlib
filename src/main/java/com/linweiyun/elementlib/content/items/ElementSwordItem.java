package com.linweiyun.elementlib.content.items;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 元素剑：命中生物后附着构造时指定的元素；右键方块可把该元素附着到环境上。
 */
public class ElementSwordItem extends Item {

    private final String elementId;

    @Nullable
    private final ResourceLocation elementKey;

    public ElementSwordItem(Properties properties, String elementId) {
        super(properties);
        this.elementId = elementId;
        this.elementKey = ResourceLocation.tryParse(elementId);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        GenshinElement element = element();
        if (element == null) {
            ElementLib.LOGGER.info("[sword-debug] {} hit {} but element()==null key={}",
                    stack.getItem(), target, this.elementKey);
            return;
        }
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ElementalAttachmentHelper.attach(EntityHost.of(target), element,
                AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK,
                itemKey == null ? elementId : itemKey.toString(), target.level().getGameTime());
        ElementLib.LOGGER.info("[sword-debug] {} hit {} -> attach {} source={}",
                stack.getItem(), target, element.getId(), itemKey);
    }

    /**
     * 右键方块：这个方块肯收下该元素就附着到环境上；不肯收就交回原版处理。
     *
     * <p>客户端不做预测，一律返回 {@code PASS} —— 收不收由服务端按方块规则表判定，
     * 免得客户端吃掉这次点击而服务端其实什么都没附着。
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        GenshinElement element = element();
        if (element == null) {
            return InteractionResult.PASS;
        }
        BlockHost host = BlockHost.of(serverLevel, context.getClickedPos());
        if (host == null) {
            return InteractionResult.PASS;
        }
        AttachResult result = ElementalAttachmentHelper.attach(host, element,
                AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
        StatusContainer container = host.container();
        if (container != null) {
            host.commit(container);
        }
        return result.attached() ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    /** 显示这把剑对应的元素名；元素未注册时提示。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag flag) {
        GenshinElement element = element();
        tooltipComponents.add(element == null
                ? Component.translatable("item.elementlib.element_sword.no_element")
                : Component.translatable(element.getTranslationKey()));
    }

    @Nullable
    private GenshinElement element() {
        return elementKey == null ? null : ModRegistries.ELEMENT_REGISTRY.get(elementKey);
    }
}