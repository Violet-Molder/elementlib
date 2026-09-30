package com.linweiyun.elementlib.core.entity;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 星璇：每 10 刻结算一次小额冰伤害，到期爆炸一次冰伤害后销毁。 */
public class StellarVortexEntity extends ElementalAreaEntity {

    public static final double RADIUS = 3.5D;
    public static final int DURATION_TICKS = 60;
    public static final int TICK_INTERVAL = 10;

    /** 周期伤害占爆炸伤害的比例（配置里没有单独的周期档位）。 */
    private static final float TICK_DAMAGE_RATIO = 0.25F;

    public StellarVortexEntity(EntityType<?> type, Level level) {
        super(type, level);
        setRadius(RADIUS);
        setDurationTicks(DURATION_TICKS);
        setTickInterval(TICK_INTERVAL);
        float base = ElementLibConfig.baseDamage("stellar_swirl_ice");
        setTickDamage(base * TICK_DAMAGE_RATIO);
        setExplodeDamage(base);
    }

    @Override
    protected ElementalReactionType tickReactionType() {
        return ModReactionTypes.STELLAR_SWIRL_ICE.get();
    }

    @Override
    protected ElementalReactionType explodeReactionType() {
        return ModReactionTypes.STELLAR_SWIRL_ICE.get();
    }

    @Nullable
    @Override
    protected GenshinElement element() {
        return ModElements.of(ModElements.CYRO);
    }
}
