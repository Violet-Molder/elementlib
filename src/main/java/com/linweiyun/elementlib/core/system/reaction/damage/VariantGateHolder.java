package com.linweiyun.elementlib.core.system.reaction.damage;

import com.linweiyun.elementlib.api.DemoContentToggles;
import com.linweiyun.elementlib.api.ElementalDamageHandler;
import com.linweiyun.elementlib.api.ReactionVariantGate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 伤害处理器与变体门的持有者，读写入口在 {@code ElementLibApi}。
 */
public final class VariantGateHolder {

    private static volatile ElementalDamageHandler damageHandler = DemoDamageHandler.INSTANCE;
    private static volatile ReactionVariantGate gate = DemoVariantGate.INSTANCE;

    private VariantGateHolder() {
    }
    public static ElementalDamageHandler damageHandler() {
        return damageHandler;
    }
    public static void setDamageHandler(@Nullable ElementalDamageHandler handler) {
        damageHandler = handler != null ? handler : DemoDamageHandler.INSTANCE;
    }
    public static ReactionVariantGate gate() {
        return gate;
    }
    public static void setGate(@Nullable ReactionVariantGate variantGate) {
        gate = variantGate != null ? variantGate : DemoVariantGate.INSTANCE;
    }
    public static final class DemoVariantGate implements ReactionVariantGate {
        public static final DemoVariantGate INSTANCE = new DemoVariantGate();

        private DemoVariantGate() {
        }

        @Override
        public boolean stellarSwirl(@Nullable Entity attacker, @Nullable LivingEntity target) {
            return DemoContentToggles.stellarSwirl();
        }

        @Override
        public boolean stellarConduce(@Nullable Entity attacker, @Nullable LivingEntity target) {
            return DemoContentToggles.stellarConduce();
        }

        @Override
        public boolean lunarCharged(@Nullable Entity attacker, @Nullable LivingEntity target) {
            return DemoContentToggles.lunarCharged();
        }

        @Override
        public String toString() {
            return "DemoVariantGate[" + DemoContentToggles.describe() + "]";
        }
    }
}
