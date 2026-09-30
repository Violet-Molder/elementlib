package com.linweiyun.elementlib.core.entity;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** 雷暴云：每 20 刻结算一次月感电伤害，到期再爆炸一次。 */
public class ThunderCloudEntity extends ElementalAreaEntity {

    public static final double RADIUS = 4.0D;
    public static final int DURATION_TICKS = 200;
    public static final int TICK_INTERVAL = 20;

    public ThunderCloudEntity(EntityType<?> type, Level level) {
        super(type, level);
        setRadius(RADIUS);
        setDurationTicks(DURATION_TICKS);
        setTickInterval(TICK_INTERVAL);
        float base = ElementLibConfig.baseDamage("lunar_charged");
        setTickDamage(base);
        setExplodeDamage(base);
    }

    @Override
    protected ElementalReactionType tickReactionType() {
        return ModReactionTypes.LUNAR_CHARGED.get();
    }

    @Override
    protected ElementalReactionType explodeReactionType() {
        return ModReactionTypes.LUNAR_CHARGED.get();
    }

    @Nullable
    @Override
    protected GenshinElement element() {
        return ModElements.of(ModElements.ELECTRO);
    }
}
