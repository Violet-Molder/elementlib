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

    /** 当前伤害处理器，永不为 {@code null}。 */
    public static ElementalDamageHandler damageHandler() {
        return damageHandler;
    }

    /** 设置伤害处理器；传 {@code null} 恢复默认处理器。 */
    public static void setDamageHandler(@Nullable ElementalDamageHandler handler) {
        damageHandler = handler != null ? handler : DemoDamageHandler.INSTANCE;
    }

    /** 当前变体门，永不为 {@code null}。 */
    public static ReactionVariantGate gate() {
        return gate;
    }

    /** 设置变体门；传 {@code null} 恢复默认门。 */
    public static void setGate(@Nullable ReactionVariantGate variantGate) {
        gate = variantGate != null ? variantGate : DemoVariantGate.INSTANCE;
    }

    /** 默认变体门：直接读 {@link DemoContentToggles}。 */
    public static final class DemoVariantGate implements ReactionVariantGate {

        /** 默认门单例。 */
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
