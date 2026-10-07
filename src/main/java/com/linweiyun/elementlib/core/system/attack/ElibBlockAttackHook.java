package com.linweiyun.elementlib.core.system.attack;

import com.linweiyun.elementlib.api.ElibAttackAction;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 方块左键交互的落点。
 *
 * <p>由 {@code PlayerInteractEvent.LeftClickBlock}（START）调用 —— 它发生在原版
 * 「创造模式直接破坏」提前 return 之前，创造模式也能触发。
 */
public final class ElibBlockAttackHook {

    private ElibBlockAttackHook() {
    }

    public static void onBlockLeftClick(Level level, BlockPos pos, Player player) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (pos == null || player == null) {
            return;
        }
        ElibAttackPipeline.dispatch(ElibAttackAction.forBlock(
                player, pos, null, AttachmentSource.NORMAL_ATTACK, AttachmentProfile.WEAK));
    }
}
