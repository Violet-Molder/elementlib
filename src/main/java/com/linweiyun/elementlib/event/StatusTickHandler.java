package com.linweiyun.elementlib.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementalTickListener;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.system.about.ColdAura;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.phys.Vec3;

/**
 * 状态的每 tick 落点 —— 推进容器，然后做两件由「容器里有什么」决定的事：
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public class StatusTickHandler {

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level().isClientSide()) return;

        StatusContainer c = living.getData(ElementalAttachments.CONTAINER);
        c.tick();

        living.setData(ElementalAttachments.CONTAINER.get(), c);
        boolean frozenWithCold = ColdAura.tick(living, c);

        freezeMotion(living, frozenWithCold);

        ElementalTickListener listener = ElementLibApi.tickListener();
        if (listener != null) {
            listener.afterElementalTick(living, c, frozenWithCold);
        }
    }

    /**
     * 冻结期间每 tick 把速度清零。
     */
    private static void freezeMotion(LivingEntity living, boolean frozenWithCold) {
        if (!frozenWithCold) {
            return;
        }
        living.setDeltaMovement(Vec3.ZERO);
        living.setSprinting(false);
        living.setDeltaMovement(Vec3.ZERO);
    }
}
