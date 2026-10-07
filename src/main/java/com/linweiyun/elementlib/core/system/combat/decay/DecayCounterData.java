package com.linweiyun.elementlib.core.system.combat.decay;

import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 计时计数器数据（无锁，线程安全）。
 */
public class DecayCounterData {


    @Getter
    private final String attackerUuid;
    @Getter
    private final String characterUuid;
    @Getter
    private final String decayTag;
    @Getter
    private final String groupId;
    @Getter
    private final DecayGroup group;
    private final AtomicInteger hitCount = new AtomicInteger(0);
    private final AtomicLong startTimeTick = new AtomicLong(0);
    private final AtomicBoolean pendingReset = new AtomicBoolean(false);
    @Getter
    private volatile long lastAccessTick;

    public DecayCounterData(String attackerUuid, String characterUuid,
                            String decayTag, String groupId,
                            DecayGroup group, long currentTick) {
        this.attackerUuid = attackerUuid;
        this.characterUuid = characterUuid;
        this.decayTag = decayTag;
        this.groupId = groupId;
        this.group = group;
        this.hitCount.set(0);
        this.startTimeTick.set(currentTick);
        this.lastAccessTick = currentTick;
    }
    public boolean checkAndResetIfTimeout(long currentTick) {
        long start = startTimeTick.get();
        lastAccessTick = currentTick;
        if (start > 0 && (currentTick - start) >= group.clearTimeTicks()) {
            hitCount.set(0);
            startTimeTick.set(currentTick);
            return true;
        }
        return false;
    }

    /**
     * 递增命中次数（仅主线程调用）。
     */
    public void incrementHitCount() {
        hitCount.incrementAndGet();
    }

    /**
     * 获取当前命中次数对应的元素量系数。
     */
    public float getElementCoefficient() {
        return group.getElementCoefficient(hitCount.get());
    }

    /**
     * 获取当前命中次数对应的伤害系数。
     */
    public float getDamageCoefficient() {
        return group.getDamageCoefficient(hitCount.get());
    }

    /**
     * 获取当前命中次数对应的削韧系数。
     */
    public float getPoiseCoefficient() {
        return group.getPoiseCoefficient(hitCount.get());
    }

    /**
     * 由Worker线程检测超时并设置标记，不修改 hitCount/startTimeTick。
     */
    public void workerCheckTimeout(long currentTick) {
        long start = startTimeTick.get();
        if (start > 0 && (currentTick - start) > group.clearTimeTicks()) {
            pendingReset.set(true);
        }
    }

    public boolean isExpiredForCleanup(long currentTick) {
        return (currentTick - startTimeTick.get()) > (group.clearTimeTicks() * 2L)
                && (currentTick - lastAccessTick) > 600L;
    }

    // ========== Getters ==========

    public int getHitCount() { return hitCount.get(); }
    public long getStartTimeTick() { return startTimeTick.get(); }

    public boolean isPendingReset() { return pendingReset.get(); }

    @Override
    public String toString() {
        return "DecayCounter{" +
                "attacker=" + attackerUuid.substring(0, 8) +
                ", char=" + characterUuid.substring(0, Math.min(characterUuid.length(), 8)) +
                ", tag=" + decayTag +
                ", group=" + groupId +
                ", count=" + hitCount.get() +
                ", reset=" + pendingReset.get() +
                '}';
    }
}
