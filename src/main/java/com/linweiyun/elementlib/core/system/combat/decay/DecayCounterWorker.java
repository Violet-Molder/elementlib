package com.linweiyun.elementlib.core.system.combat.decay;

import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计时计数器 Worker 线程 —— 独立于主线程运行。
 */
public class DecayCounterWorker {
    private static volatile DecayCounterWorker instance;
    private ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<DecayCounterManager> registeredManagers = new LinkedBlockingQueue<>();
    private volatile TickProvider tickProvider;
    private final AtomicLong totalScans = new AtomicLong(0);
    private final AtomicLong totalResets = new AtomicLong(0);
    private final AtomicLong totalCleanups = new AtomicLong(0);
    @FunctionalInterface
    public interface TickProvider {
        long getCurrentTick();
    }

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
        executor.scheduleAtFixedRate(this::scanTimeout, 50, 2, TimeUnit.MILLISECONDS);
        executor.scheduleAtFixedRate(this::cleanupExpired, 5, 5, TimeUnit.SECONDS);
    }
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
    public void registerManager(DecayCounterManager manager) {
        if (running.get() && manager != null) {
            registeredManagers.offer(manager);
        }
    }
    public void registerManagers(List<DecayCounterManager> managers) {
        if (running.get() && managers != null) {
            registeredManagers.addAll(managers);
        }
    }
    public void unregisterManager(DecayCounterManager manager) {
        registeredManagers.remove(manager);
    }
    private boolean isOwnerValid(DecayCounterManager manager) {
        try {
            LivingEntity owner = manager.getOwner();
            return owner != null && owner.isAlive();
        } catch (Exception e) {
            return false;
        }
    }
    private void scanTimeout() {
        if (!running.get() || tickProvider == null) return;
        long currentTick = tickProvider.getCurrentTick();
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

            }
        }

        totalScans.incrementAndGet();
        totalResets.addAndGet(totalResetsThisRound);
    }
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

            }
        }

        totalCleanups.addAndGet(totalCleanupsThisRound);
    }

    public boolean isRunning() { return running.get(); }
    public long getTotalScans() { return totalScans.get(); }
    public long getTotalResets() { return totalResets.get(); }
    public long getTotalCleanups() { return totalCleanups.get(); }
    public int getRegisteredManagerCount() { return registeredManagers.size(); }

    public String getStats() {
        return String.format("DecayWorker[scans=%d, resets=%d, cleanups=%d, managers=%d]",
                totalScans.get(), totalResets.get(), totalCleanups.get(),
                registeredManagers.size());
    }
}
