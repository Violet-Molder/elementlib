package com.linweiyun.elementlib.core.system.combat.decay;

import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 目标实体上的计时计数器管理器。
 */
public class DecayCounterManager {
    private final ConcurrentMap<String, DecayCounterData> counters = new ConcurrentHashMap<>();
    @Getter
    private final LivingEntity owner;
    @Getter
    private volatile long lastWorkerScanTick;
    @Getter
    private volatile int pendingResetCount;

    public DecayCounterManager(LivingEntity owner) {
        this.owner = owner;
        this.lastWorkerScanTick = 0;
    }

    /**
     * 处理一次攻击命中（主线程调用）。
     */
    public DecayResult processHit(LivingEntity attacker, @Nullable String characterKey,
                                  DecaySpec spec, long currentTick) {
        String counterKey = buildCounterKey(attacker, characterKey, spec);
        if (counterKey == null) {
            return DecayResult.NONE;
        }

        DecayCounterData counter = counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, characterKey, spec, currentTick));
        counter.checkAndResetIfTimeout(currentTick);

        float elementCoef = counter.getElementCoefficient();
        counter.incrementHitCount();

        return new DecayResult(elementCoef);
    }
    public DecayCounterData getOrCreateCounter(LivingEntity attacker, @Nullable String characterKey,
                                               DecaySpec spec, long currentTick) {
        String counterKey = buildCounterKey(attacker, characterKey, spec);
        if (counterKey == null) return null;
        return counters.computeIfAbsent(counterKey,
                k -> createCounter(attacker, characterKey, spec, currentTick));
    }

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
    public int getCounterCount() { return counters.size(); }

}
