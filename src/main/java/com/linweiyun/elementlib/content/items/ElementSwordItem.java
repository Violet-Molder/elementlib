package com.linweiyun.elementlib.content.items;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.api.ElibAttackTrigger;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.attack.ElibAttackPipeline;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 元素剑：命中生物后附着构造时指定的元素；右键用射线检测把元素附着到环境（方块/流体）。
 */
public class ElementSwordItem extends SwordItem {

    private final String elementId;

    @Nullable
    private final ResourceLocation elementKey;

    public ElementSwordItem(Tier tier, Properties properties, String elementId) {
        super(tier, properties);
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
        // 统一攻击入口：打实体也算一次"攻击"，附着交给管线（方块/实体同一条路）
        ElibAttackPipeline.dispatchOn(ElibAttackAction.of(attacker, element,
                ElibAttackTrigger.ENTITY, AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK, 3.0), target);
    }

    /**
     * 右键（空手目标不明时/流体）：用射线检测决定附着到哪个目标。
     * <p>先测实体，再测方块（OUTLINE，挡住时才命中），最后测流体 —— 这样水、岩壁、生物都能被
     * 一张射线检测覆盖，绕开 {@code useOn} 选不中水的限制。
     *
     * <p>只在服务端执行；里面不做位置预测。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }
        GenshinElement element = element();
        if (element == null) {
            return InteractionResultHolder.pass(stack);
        }

        double range = 5.0D;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.multiply(range, range, range));

        // 1) 实体
        AABB searchBox = player.getBoundingBox().expandTowards(look.multiply(range, range, range)).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player, eye, end, searchBox,
                e -> e instanceof LivingEntity, range * range);
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            // 统一攻击入口：打实体也算一次"攻击"，附着交给管线（方块/实体同一条路）
            ElibAttackPipeline.dispatchOn(ElibAttackAction.of(player, element,
                    ElibAttackTrigger.ENTITY, AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK, 3.0), living);
            return InteractionResultHolder.sidedSuccess(stack, false);
        }

        // 2) 方块（OUTLINE：只有挡住的方块才命中）
        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            BlockHost host = BlockHost.of(serverLevel, blockHit.getBlockPos());
            if (host != null) {
                AttachResult result = ElementalAttachmentHelper.attach(host, element,
                        AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
                StatusContainer container = host.container();
                if (container != null) {
                    host.commit(container);
                }
                return InteractionResultHolder.sidedSuccess(stack, false);
            }
        }

        return InteractionResultHolder.pass(stack);
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
    public GenshinElement element() {
        return elementKey == null ? null : ModRegistries.ELEMENT_REGISTRY.get(elementKey);
    }
}
