package com.linweiyun.elementlib.event;

import com.linweiyun.elementlib.ElementLib;
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
 * 寒元素的伴随同步（{@link ColdAura}）与冻结期间的位移锁。
 *
 * <p>两件事读同一份判据（同一 tick 的容器扫描结果），不会出现两份真相。
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

        // 寒的伴随 + 效果（减速 / 禁 AI）都在这一处重算；返回值即「有寒且有冻」
        boolean frozenWithCold = ColdAura.tick(living, c);

        freezeMotion(living, frozenWithCold);
    }

    /**
     * 冻结期间每 tick 把速度清零。
     *
     * <p>冻结只做了 {@code setNoAi(true)}，水流推动、流体浮力等物理仍会改服务端的
     * deltaMovement，而客户端因 NoAi 不预测这段位移，两边位置会分叉（表现为解冻瞬间目标弹回）。
     *
     * <p>判据用「有寒且有冻」：冻元素没被宿主接受寒的元素生物本就不该被冻住，位移自然也不该锁。
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
