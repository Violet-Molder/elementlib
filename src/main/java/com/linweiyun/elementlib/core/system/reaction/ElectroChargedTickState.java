package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.reaction.builtin.ElectroChargedReaction;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Setter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 感电的周期结算状态：激活后每 {@value #TICK_INTERVAL} 刻各消耗水雷
 */
public class ElectroChargedTickState implements IPersistedSerializable {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    private static final float CONSUME_PER_TICK = 0.4f;
    private static final int TICK_INTERVAL = 20;
    private static final double CHAIN_RADIUS = 5.0;

    @Persisted(key = "ec_active")
    private boolean active;

    @Persisted(key = "ec_tick_counter")
    private int tickCounter;

    @Persisted(key = "ec_attacker_uuid")
    private UUID attackerUUID;

    @Setter
    private transient StatusContainer container;
    private transient LivingEntity targetEntity;

    public ElectroChargedTickState() {
        this.active = false;
        this.tickCounter = 0;
        this.attackerUUID = null;
    }

    public void onActiveTrigger(@Nullable Entity attacker, LivingEntity target) {
        this.active = true;
        this.tickCounter = 0;
        if (attacker != null) {
            this.attackerUUID = attacker.getUUID();
        }
        this.targetEntity = target;
        dealDamage(target, false);
    }

    public void onTick() {
        if (container == null) return;

        if (!active) {
            tryAutoActivate();
            if (!active) return;
        }

        tickCounter++;
        if (tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;

        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) {
            active = false;
            return;
        }

        GenshinElement hydro = ElementRoles.of(ElementRoles.HYDRO);
        GenshinElement electro = ElementRoles.of(ElementRoles.ELECTRO);
        if (hydro == null || electro == null) {
            active = false;
            return;
        }

        ElementalAttachmentInstance hydroInst = ElectroChargedReaction.findElement(container, hydro);
        ElementalAttachmentInstance electroInst = ElectroChargedReaction.findElement(container, electro);

        if (hydroInst == null || electroInst == null) {
            active = false;
            return;
        }

        float hydroBefore = hydroInst.getUnit();
        float electroBefore = electroInst.getUnit();

        if (hydroBefore <= 0 || electroBefore <= 0) {
            active = false;
            return;
        }

        hydroInst.consume(Math.min(CONSUME_PER_TICK, hydroBefore));
        electroInst.consume(Math.min(CONSUME_PER_TICK, electroBefore));

        dealDamage(target, true);

        if (hydroInst.getUnit() <= 0 || electroInst.getUnit() <= 0) {
            active = false;
        }
    }

    private void tryAutoActivate() {
        if (ReactionPriorityCalculator.hasFrozen(container)) return;

        LivingEntity target = resolveTarget();
        if (target == null || !target.isAlive()) return;
        // 月感电顶替感电时不自动激活
        if (com.linweiyun.elementlib.api.ElementLibApi.variantGate().lunarCharged(null, target)) return;

        GenshinElement hydro = ElementRoles.of(ElementRoles.HYDRO);
        GenshinElement electro = ElementRoles.of(ElementRoles.ELECTRO);
        if (hydro == null || electro == null) return;

        ElementalAttachmentInstance hydroInst = ElectroChargedReaction.findElement(container, hydro);
        ElementalAttachmentInstance electroInst = ElectroChargedReaction.findElement(container, electro);

        if (hydroInst != null && electroInst != null
                && hydroInst.getUnit() > 0 && electroInst.getUnit() > 0) {
            active = true;
            tickCounter = TICK_INTERVAL - 1;
            targetEntity = target;
            LOGGER.debug("[感电自激活] target={}", target.getName().getString());
        }
    }

    private void dealDamage(LivingEntity target, boolean chain) {
        LivingEntity attacker = resolveAttacker();
        LivingEntity calcAttacker = attacker != null ? attacker : target;

        LOGGER.debug("[感电触发] target={} | chain={} | calcAttacker={}",
                target.getName().getString(), chain, calcAttacker.getName().getString());

        GenshinElement electro = ElementRoles.of(ElementRoles.ELECTRO);
        ElementalReactionType type = electroChargedType();
        if (electro == null || type == null) return;

        ReactionDamage.dealDirect(type, electro, attacker, target,
                ElementLibConfig.baseDamage("electro_charged"));
        ReactionFeedback.transformative(target, type, electro);

        if (chain) {
            chainNearbyWet(target, attacker, electro);
        }
    }

    /** 给半径 {@value #CHAIN_RADIUS} 格内、身上有水附着的其它实体各结算一次感电。 */
    private void chainNearbyWet(LivingEntity source, @Nullable LivingEntity attacker, GenshinElement electro) {
        if (!(source.level() instanceof ServerLevel level)) return;
        GenshinElement hydro = ElementRoles.of(ElementRoles.HYDRO);
        ElementalReactionType type = electroChargedType();
        if (hydro == null || type == null) return;

        double rSq = CHAIN_RADIUS * CHAIN_RADIUS;

        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                source.getBoundingBox().inflate(CHAIN_RADIUS),
                e -> e != source && e.isAlive() && source.distanceToSqr(e) <= rSq)) {

            StatusContainer nc = nearby.getData(ElementalAttachments.CONTAINER);

            ElementalAttachmentInstance h = ElectroChargedReaction.findElement(nc, hydro);
            if (h != null && h.getUnit() > 0) {
                ReactionDamage.dealDirect(type, electro, source, nearby,
                        ElementLibConfig.baseDamage("electro_charged"));
                ReactionFeedback.transformative(nearby, type, electro);
            }
        }
    }

    /** 感电的反应类型；示范反应未注册时为 {@code null}。 */
    @Nullable
    private static ElementalReactionType electroChargedType() {
        DeferredHolder<ElementalReactionType, ElementalReactionType> holder = ModReactionTypes.ELECTRO_CHARGED;
        return holder.isBound() ? holder.get() : null;
    }

    @Nullable
    private LivingEntity resolveTarget() {
        if (targetEntity != null && targetEntity.isAlive()) return targetEntity;
        if (container != null) {
            for (StatusInstance inst : container.getAll()) {
                if (inst instanceof ElementalAttachmentInstance ea && ea.getOwner() != null) {
                    return ea.getOwner();
                }
            }
        }
        return null;
    }

    @Nullable
    private LivingEntity resolveAttacker() {
        if (attackerUUID == null) return null;
        LivingEntity target = resolveTarget();
        if (target != null && target.level() instanceof ServerLevel level
                && level.getEntity(attackerUUID) instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}
