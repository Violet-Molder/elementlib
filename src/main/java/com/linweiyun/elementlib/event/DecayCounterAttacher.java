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
 *
 * <p>计数器是懒建的，所以这里每 tick 只检查<b>已经有管理器</b>的实体（{@code hasData}，
 * 不会创建附件），首次见到时登记一次。
 *
 * <p>重复登记必须挡住：{@code DecayCounterWorker.registerManager} 只是往队列里 offer，
 * 队列在超时扫描时不排空，每 tick 都登记会让队列无限增长。因此用<b>弱键 Set</b> 记住已登记的管理器，
 * 实体消失后条目自然被 GC 掉。
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

        // 只处理「已经有计数器管理器」的实体：hasData 不会创建附件
        if (!living.hasData(ElementalAttachments.DECAY_COUNTER.get())) return;

        DecayCounterManager manager = living.getData(ElementalAttachments.DECAY_COUNTER);
        if (manager == null) return;

        DecayCounterWorker worker = DecayCounterWorker.getInstance();
        if (!worker.isRunning()) return;

        // 首次见到才登记；登记成功才记账，避免 Worker 未启动时把管理器标记成「已登记」。
        if (REGISTERED.add(manager)) {
            worker.registerManager(manager);
        }
    }
}
