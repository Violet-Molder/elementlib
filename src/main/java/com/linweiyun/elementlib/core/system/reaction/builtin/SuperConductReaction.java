package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.system.performance.BoundedLruMap;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionFeedback;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 超导反应 —— 雷 + 冰，剧变反应，注册比 1:1 双方同时消耗。
 */
public class SuperConductReaction extends ElementalReaction {

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    private static final int DAMAGE_COOLDOWN_TICKS = 10;
    private static final Map<UUID, Long> lastDamageTick = BoundedLruMap.create();

    public SuperConductReaction(Supplier<ElementalReactionType> reactionType,
                                String elementAId, String elementBId,
                                float ratioA, float ratioB, int basePriority) {
        super(reactionType, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public ReactionResult execute(ReactionContext context) {
        GenshinElement elA = getElementA();
        GenshinElement elB = getElementB();
        GenshinElement attackerMain = context.attackerElement().getMainElement();
        boolean attackerIsA = (attackerMain == elA);

        GenshinElement defenderTarget = attackerIsA ? elB : elA;
        float totalDefenderUnit = sumConsumable(context.targetContainer(), defenderTarget);
        if (totalDefenderUnit <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        float attackerUnit = context.attackerUnit();
        float[] consumed = attackerIsA
                ? calculateConsumption(attackerUnit, totalDefenderUnit)
                : calculateConsumption(totalDefenderUnit, attackerUnit);
        float consumedA = consumed[0];
        float consumedB = consumed[1];
        if (consumedA <= 0f || consumedB <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        consumeElementUnit(context.targetContainer(), elB, consumedB);
        consumeElementUnit(context.targetContainer(), elA, consumedA);

        applyDamageOffCooldown(context);

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(attackerIsA ? consumedA : consumedB)
                .consumedDefender(attackerIsA ? consumedB : consumedA)
                .build();
    }

    private static void applyDamageOffCooldown(ReactionContext context) {
        LivingEntity target = context.targetEntity();
        if (target == null) return;

        long gameTime = target.level().getGameTime();
        Long last = lastDamageTick.get(target.getUUID());
        if (last != null && gameTime - last < DAMAGE_COOLDOWN_TICKS) {
            return;
        }
        lastDamageTick.put(target.getUUID(), gameTime);

        Entity attacker = context.attackerEntity();
        GenshinElement cyro = ModElements.of(ModElements.CYRO);

        if (ElementLibApi.variantGate().stellarConduce(attacker, target)) {
            ReactionDamage.dealDirect(ModReactionTypes.STELLAR_CONDUCE_ELECTRO.get(),
                    ModElements.of(ModElements.ELECTRO), attacker, target,
                    ElementLibConfig.baseDamage("stellar_conduce_electro"));
            ReactionDamage.dealDirect(ModReactionTypes.STELLAR_CONDUCE_ICE.get(),
                    cyro, attacker, target,
                    ElementLibConfig.baseDamage("stellar_conduce_ice"));
        } else {
            ReactionDamage.dealDirect(ModReactionTypes.SUPERCONDUCT.get(), cyro,
                    attacker, target, ElementLibConfig.baseDamage("superconduct"));
        }

        ReactionFeedback.transformative(target, ModReactionTypes.SUPERCONDUCT.get(), cyro);
    }
}
