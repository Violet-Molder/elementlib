package com.linweiyun.elementlib.core.system.combat.decay;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 目标实体上的计时计数器管理器。
 *
 * <p>每个 LivingEntity 持有自己的实例（经 {@link IDecayCounterHolder} 获取）。
 * 主线程调用 {@link #processHit}；Worker 线程调用 {@link #workerScanTimeout} 与
 * {@link #workerCleanup}。CounterKey 格式：{@code attackerUuid:characterKey:decayTag:groupId}。
 */
public class DecayCounterManager {

    // 计数器存储 - ConcurrentHashMap支持并发访问
    private final ConcurrentMap<String, DecayCounterData> counters = new ConcurrentHashMap<>();

    // 所属目标实体
    private final LivingEntity owner;

    // 统计信息（供Worker线程读取）
    private volatile long lastWorkerScanTick;
    private volatile int pendingResetCount;

    public DecayCounterManager(LivingEntity owner) {
        this.owner = owner;
        this.lastWorkerScanTick = 0;
    }

    // ========== 主线程API ==========

    /**
     * 处理一次攻击命中（主线程调用）。
     *
     * @param characterKey 角色/来源标识，null 时按「直接来源」处理
     * @return 衰减结果（元素量系数）
     */
    public DecayResult processHit(LivingEntity attacker, @Nullable String characterKey,
                                  DecaySpec spec, long currentTick) {
        // 1. 构建key并获取/创建计数器
        String counterKey = buildCounterKey(attacker, characterKey, spec);
        if (counterKey == null) {
            return DecayResult.NONE;
        }

        DecayCounterData counter = counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, characterKey, spec, currentTick));

        // 2. 检查并执行超时计数器重置
        counter.checkAndResetIfTimeout(currentTick);


        // 3. 读取当前计数对应的系数
        float elementCoef = counter.getElementCoefficient();

        // 4. 递增计数
        counter.incrementHitCount();

        return new DecayResult(elementCoef);
    }

    /**
     * 获取或创建计数器（仅查询，不处理攻击流程）。
     */
    public DecayCounterData getOrCreateCounter(LivingEntity attacker, @Nullable String characterKey,
                                               DecaySpec spec, long currentTick) {
        String counterKey = buildCounterKey(attacker, characterKey, spec);
        if (counterKey == null) return null;
        return counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, characterKey, spec, currentTick));
    }

    // ========== Worker线程API ==========

    /**
     * Worker 线程扫描超时计数器，只设置 pendingReset 标记。
     *
     * @return 本次扫描设置了多少个重置标记
     */
    public int workerScanTimeout(long currentTick) {
        int count = 0;
        for (DecayCounterData counter : counters.values()) {
            if (counter.isPendingReset()) continue;
            counter.workerCheckTimeout(currentTick);
            if (counter.isPendingReset()) {
                count++;
            }
        }
        this.pendingResetCount = count;
        this.lastWorkerScanTick = currentTick;
        return count;
    }

    /**
     * Worker 线程清理过期计数器。
     *
     * @return 清理了多少个计数器
     */
    public int workerCleanup(long currentTick) {
        int removed = 0;
        var iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().isExpiredForCleanup(currentTick)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    // ========== 内部方法 ==========

    private String buildCounterKey(LivingEntity attacker, @Nullable String characterKey, DecaySpec spec) {
        String decayTag = spec.decayTag();
        if (decayTag == null) return null;

        String attackerUuid = attacker.getUUID().toString();
        String characterUuid = characterKey != null
                ? characterKey
                : "direct:" + attackerUuid;
        String groupId = groupKeyOf(spec.group());

        return attackerUuid + ":" + characterUuid + ":" + decayTag + ":" + groupId;
    }

    /**
     * 衰减组的<b>身份</b>（进计数器 key 的那一段），默认普攻组固定为 "default"，其余每组各一份。
     *
     * <p>用身份哈希当身份，因为 {@link DecayGroup} 里的 {@link DecaySequence} 没有重写
     * {@code equals}；计数器只在运行时存活（不落存档），跨重启不需要稳定。
     */
    private static String groupKeyOf(DecayGroup group) {
        if (group == DecayGroups.DEFAULT_NORMAL_ATTACK) return "default";
        return "g" + Integer.toHexString(System.identityHashCode(group));
    }

    private DecayCounterData createCounter(LivingEntity attacker, @Nullable String characterKey,
                                           DecaySpec spec, long currentTick) {
        String attackerUuid = attacker.getUUID().toString();
        String characterUuid = characterKey != null
                ? characterKey
                : "direct:" + attackerUuid;
        String decayTag = spec.decayTag();
        String groupId = groupKeyOf(spec.group());

        return new DecayCounterData(attackerUuid, characterUuid,
                decayTag, groupId, spec.group(), currentTick);
    }

    // ========== Getters ==========

    public LivingEntity getOwner() { return owner; }
    public int getCounterCount() { return counters.size(); }
    public int getPendingResetCount() { return pendingResetCount; }
    public long getLastWorkerScanTick() { return lastWorkerScanTick; }
}
