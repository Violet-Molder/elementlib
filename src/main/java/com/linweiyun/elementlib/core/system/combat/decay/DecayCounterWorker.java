package com.linweiyun.elementlib.core.system.combat.decay;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计时计数器 Worker 线程 —— 独立于主线程运行。
 *
 * <p>定期扫描已注册实体的计数器：超时检测每 100ms 一次，清理每 5 秒一次。
 * 单线程顺序执行，经 {@link TickProvider} 取 tick（无需主线程同步）。
 */
public class DecayCounterWorker {

    // Worker线程实例
    private static volatile DecayCounterWorker instance;

    // 执行器，即定时任务
    private ScheduledExecutorService executor;

    // 运行状态
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 已注册的实体管理器队列（主线程添加，Worker线程读取）
    private final BlockingQueue<DecayCounterManager> registeredManagers = new LinkedBlockingQueue<>();

    // Worker获取tick的提供者（由主线程设置）
    private volatile TickProvider tickProvider;

    // 统计数据
    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong totalResets = new AtomicLong(0);
    private final AtomicLong totalCleanups = new AtomicLong(0);

    // ========== Tick提供者接口 ==========

    /**
     * Tick提供者 - 由主线程实现，提供当前tick值
     */
    @FunctionalInterface
    public interface TickProvider {
        long getCurrentTick();
    }

    // ========== 单例获取 ==========

    public static DecayCounterWorker getInstance() {
        if (instance == null) {
            synchronized (DecayCounterWorker.class) {
                if (instance == null) {
                    instance = new DecayCounterWorker();
                }
            }
        }
        return instance;
    }

    // ========== 生命周期 ==========

    /**
     * 启动Worker线程。
     */
    public void start(TickProvider tickProvider) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        this.tickProvider = tickProvider;

        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "DecayCounter-Worker");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        };

        this.executor = Executors.newScheduledThreadPool(2, factory);

        // 超时检测任务：每100ms（2 tick）扫描一次
        executor.scheduleAtFixedRate(this::scanTimeout, 50, 2, TimeUnit.MILLISECONDS);

        // 清理任务：每5秒清理一次
        executor.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 停止Worker线程。
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        registeredManagers.clear();
    }

    /**
     * 注册一个实体的计数器管理器（主线程调用，Worker 线程异步处理）。
     */
    public void registerManager(DecayCounterManager manager) {
        if (running.get() && manager != null) {
            registeredManagers.offer(manager);
        }
    }

    /**
     * 批量注册。
     */
    public void registerManagers(List<DecayCounterManager> managers) {
        if (running.get() && managers != null) {
            registeredManagers.addAll(managers);
        }
    }

    /**
     * 注销一个实体的计数器管理器。
     */
    public void unregisterManager(DecayCounterManager manager) {
        registeredManagers.remove(manager);
    }

    // ========== Worker任务 ==========

    /**
     * 判断manager的owner实体是否还活着且有效
     */
    private boolean isOwnerValid(DecayCounterManager manager) {
        try {
            LivingEntity owner = manager.getOwner();
            return owner != null && owner.isAlive() && owner.level() != null;
        } catch (Exception e) {
            return false;
        }
    }
    private void scanTimeout() {
        if (!running.get() || tickProvider == null) return;

        long currentTick = tickProvider.getCurrentTick();

        // 批量取出所有管理器
        DecayCounterManager[] managers = registeredManagers.toArray(new DecayCounterManager[0]);

        int totalResetsThisRound = 0;
        for (DecayCounterManager manager : managers) {
            try {
                if (!isOwnerValid(manager)) {
                    registeredManagers.remove(manager);
                    continue;
                }
                totalResetsThisRound += manager.workerScanTimeout(currentTick);
            } catch (Exception e) {
                // 单个管理器异常不影响其他
            }
        }

        totalScans.incrementAndGet();
        totalResets.addAndGet(totalResetsThisRound);
    }

    /**
     * 清理过期任务
     */
    private void cleanupExpired() {
        if (!running.get() || tickProvider == null) return;

        long currentTick = tickProvider.getCurrentTick();
        DecayCounterManager[] managers = registeredManagers.toArray(new DecayCounterManager[0]);

        int totalCleanupsThisRound = 0;
        for (DecayCounterManager manager : managers) {
            try {
                if (!isOwnerValid(manager)) {
                    registeredManagers.remove(manager);
                    continue;
                }
                totalCleanupsThisRound += manager.workerCleanup(currentTick);
            } catch (Exception e) {
                // 单个管理器异常不影响其他
            }
        }

        totalCleanups.addAndGet(totalCleanupsThisRound);
    }

    // ========== 状态查询 ==========

    public boolean isRunning() { return running.get(); }
    public long getTotalScans() { return totalScans.get(); }
    public long getTotalResets() { return totalResets.get(); }
    public long getTotalCleanups() { return totalCleanups.get(); }
    public int getRegisteredManagerCount() { return registeredManagers.size(); }

    /**
     * 获取统计快照
     */
    public String getStats() {
        return String.format("DecayWorker[scans=%d, resets=%d, cleanups=%d, managers=%d]",
                totalScans.get(), totalResets.get(), totalCleanups.get(),
                registeredManagers.size());
    }
}
