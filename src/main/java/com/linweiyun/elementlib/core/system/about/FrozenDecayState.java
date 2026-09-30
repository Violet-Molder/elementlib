package com.linweiyun.elementlib.core.system.about;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import org.slf4j.Logger;

/**
 * 冻元素动态衰减状态 —— 挂在 StatusContainer 上，独立于具体的 FROZEN 实例。
 *
 * <p>衰减规则：冻结中每秒衰减率 {@code +0.1}（0.4 → 0.5 → 0.6 …）；
 * 脱离冻结后每秒 {@code -0.2}，直到回到 0.4。
 * 挂在容器上是为了让所有 FROZEN 消失后恢复逻辑继续运行，新生成的 FROZEN 会继承
 * 尚未恢复完的衰减率 —— 连续冻结因此越来越短。
 */
public class FrozenDecayState implements IPersistedSerializable {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    /** 初始/最小衰减率：每秒衰减 0.4 元素量 */
    public static final float MIN_DECAY_RATE = 0.4f;

    /** 冻结中每秒衰减率增长量 */
    public static final float GROW_PER_SECOND = 0.1f;

    /** 脱离冻结后每秒衰减率恢复量 */
    public static final float RECOVER_PER_SECOND = 0.2f;

    @Persisted(key = "frozen_current_decay")
    private float currentDecayRate;

    /** tick 计数器：累计到 20 才真正改一次 decayRate（20 tick = 1秒） */
    @Persisted(key = "frozen_tick_counter")
    private int tickCounter;
    /** 是否活跃：只有衰减率被抬升到 MIN 以上过，或者当前有 FROZEN 时才是 true */
    @Persisted(key = "frozen_active")
    private boolean active;


    public FrozenDecayState() {
        this.currentDecayRate = MIN_DECAY_RATE;
    }
    /**
     * 冻结反应生成 FROZEN 时调用，激活衰减状态让 {@link #onTick} 开始工作。
     */
    public void activate() {
        this.active = true;
    }

    /**
     * 每 tick 都可以调，内部只有凑够 20 tick（1秒）才真正增减衰减率。
     *
     * @param hasFrozenAlive 当前容器里是否有 FROZEN 实例活着
     */
    public void onTick(boolean hasFrozenAlive) {
        // 不活跃就直接跳过（从未冻结过，或已恢复完毕且无 FROZEN）
        if (!active) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (hasFrozenAlive) {
            currentDecayRate += GROW_PER_SECOND;
        } else {
            // 已经回到 MIN 就不再递减，直接标记非活跃，彻底停掉
            if (currentDecayRate <= MIN_DECAY_RATE) {
                currentDecayRate = MIN_DECAY_RATE;
                active = false;
                return;
            }
            currentDecayRate -= RECOVER_PER_SECOND;
            if (currentDecayRate <= MIN_DECAY_RATE) {
                currentDecayRate = MIN_DECAY_RATE;
                active = false;
            }
        }
    }

    public float getCurrentDecayRate() {
        return currentDecayRate;
    }
}
