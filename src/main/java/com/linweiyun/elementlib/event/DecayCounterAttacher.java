package com.linweiyun.elementlib.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterManager;
import com.linweiyun.elementlib.core.system.combat.decay.DecayCounterWorker;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 计时计数器的注册落点 —— 把「这个生物身上有计数器管理器」告诉 Worker 线程。
 */
@EventBusSubscriber(modid = ElementLib.MOD_ID)
public final class DecayCounterAttacher {

    /** 已登记给 Worker 的管理器（弱引用键，实体消失后自动清理）。 */
    private static final Set<DecayCounterManager> REGISTERED =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<DecayCounterManager, Boolean>()));

    private DecayCounterAttacher() {
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living.level() == null || living.level().isClientSide()) return;
        if (living.isRemoved()) return;

        if (!living.hasData(ElementalAttachments.DECAY_COUNTER.get())) return;

        DecayCounterManager manager = living.getData(ElementalAttachments.DECAY_COUNTER);
        if (manager == null) return;

        DecayCounterWorker worker = DecayCounterWorker.getInstance();
        if (!worker.isRunning()) return;

        if (REGISTERED.add(manager)) {
            worker.registerManager(manager);
        }
    }
}
