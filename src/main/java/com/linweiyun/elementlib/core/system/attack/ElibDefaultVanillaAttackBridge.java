package com.linweiyun.elementlib.core.system.attack;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.api.ElibAttackTrigger;
import com.linweiyun.elementlib.content.items.ElementSwordItem;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 默认桥：把原版「左键攻击实体」喂进攻击管线，让只用 elementlib 的 mod 也能扩展。
 *
 * <p>MineGenshin 会关掉它（自己的动作系统已经 dispatch），避免同一次命中进管线两次。
 * 方块左键不走这里，走 {@code BlockStateAttackMixin}（那条路在保护判断之后）。
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public final class ElibDefaultVanillaAttackBridge {

    private static final double DEFAULT_REACH = 3.0;

    private static volatile boolean enabled = true;

    private ElibDefaultVanillaAttackBridge() {
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        // 元素剑自己在 postHurtEnemy 里 dispatch，避免同一次命中触发两遍
        if (player.getMainHandItem().getItem() instanceof ElementSwordItem) {
            return;
        }
        ElibAttackPipeline.dispatchOn(ElibAttackAction.of(player, null, ElibAttackTrigger.ENTITY,
                AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK, DEFAULT_REACH), event.getTarget());
    }

    /** 方块左键：START 时触发一次；在创造模式提前 return 之前，创造模式也生效。 */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        if (!level.mayInteract(player, pos)
                || !player.canInteractWithBlock(pos, 1.0)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel
                && serverLevel.getServer() != null
                && serverLevel.getServer().isUnderSpawnProtection(serverLevel, pos, player)) {
            return;
        }
        ElibBlockAttackHook.onBlockLeftClick(level, pos, player);
    }
}
