package com.linweiyun.elementlib.core.system.about;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import lombok.Getter;
import org.slf4j.Logger;

/**
 * 冻元素动态衰减状态 —— 挂在 StatusContainer 上，独立于具体的 FROZEN 实例。
 */
public class FrozenDecayState implements IPersistedSerializable {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    public static final float MIN_DECAY_RATE = 0.4f;
    public static final float GROW_PER_SECOND = 0.1f;
    public static final float RECOVER_PER_SECOND = 0.2f;

    @Getter
    @Persisted(key = "frozen_current_decay")
    private float currentDecayRate;
    @Persisted(key = "frozen_tick_counter")
    private int tickCounter;
    @Persisted(key = "frozen_active")
    private boolean active;


    public FrozenDecayState() {
        this.currentDecayRate = MIN_DECAY_RATE;
    }
    public void activate() {
        this.active = true;
    }
    public void onTick(boolean hasFrozenAlive) {
        // 不活跃就直接跳过（从未冻结过，或已恢复完毕且无 FROZEN）
        if (!active) return;

        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        if (hasFrozenAlive) {
            currentDecayRate += GROW_PER_SECOND;
        } else {
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

}
