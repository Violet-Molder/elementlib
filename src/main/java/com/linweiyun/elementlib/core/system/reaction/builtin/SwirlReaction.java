package com.linweiyun.elementlib.core.system.reaction.builtin;

import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.entity.ElementalAreaEntity;
import com.linweiyun.elementlib.core.entity.ModEntities;
import com.linweiyun.elementlib.core.entity.StellarVortexEntity;
import com.linweiyun.elementlib.core.system.about.AttachContext;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.core.system.performance.BoundedLruMap;
import com.linweiyun.elementlib.core.system.reaction.ElementalReaction;
import com.linweiyun.elementlib.core.system.reaction.ReactionContext;
import com.linweiyun.elementlib.core.system.reaction.ReactionFeedback;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.core.system.registry.register.ModReactionTypes;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 扩散反应 —— 风 + 可扩散元素（火 / 水 / 雷 / 冰），剧变反应。
 */
public class SwirlReaction extends ElementalReaction {
    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    public static final float WEAK_SWIRL_SPREAD = 2.2f;
    public static final float STRONG_SWIRL_SPREAD = 3.4f;

    private static final int SWIRL_COOLDOWN_TICKS = 20;
    private static final double SWIRL_RADIUS = 5.0;
    private static final float STELLAR_VORTEX_TICK_DAMAGE_RATIO = 0.25f;
    private static final Map<UUID, Long> lastSwirlTick = BoundedLruMap.create();

    private static final String PYRO_ID = ModElements.PYRO.getId().toString();
    private static final String HYDRO_ID = ModElements.HYDRO.getId().toString();
    private static final String ELECTRO_ID = ModElements.ELECTRO.getId().toString();
    private static final String CYRO_ID = ModElements.CYRO.getId().toString();
    private static final String ANEMO_ID = ModElements.ANEMO.getId().toString();
    private static final String FROZEN_ID = ModElements.FROZEN.getId().toString();

    private static final Set<String> SWIRLABLE_IDS = Set.of(PYRO_ID, HYDRO_ID, ELECTRO_ID, CYRO_ID);
    private static final List<String> SPREAD_PRIORITY_IDS = List.of(PYRO_ID, HYDRO_ID, ELECTRO_ID, CYRO_ID);

    public SwirlReaction(Supplier<ElementalReactionType> type,
                         String elementAId, String elementBId,
                         float ratioA, float ratioB, int basePriority) {
        super(type, elementAId, elementBId, ratioA, ratioB, basePriority);
    }

    @Override
    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        GenshinElement defenderMain = defenderElement.getMainElement();
        if (attackerMain == null || defenderMain == null) return false;

        GenshinElement anemo = ModElements.of(ModElements.ANEMO);
        return attackerMain == anemo && isSwirlable(defenderMain);
    }

    @Override
    public boolean isBlocked(ReactionContext context) {
        if (context.targetEntity() == null) return true;
        long gameTime = context.targetEntity().level().getGameTime();
        UUID targetId = context.targetEntity().getUUID();
        Long last = lastSwirlTick.get(targetId);
        return last != null && gameTime - last < SWIRL_COOLDOWN_TICKS;
    }

    @Override
    public ReactionResult execute(ReactionContext ctx) {
        GenshinElement anemoEl = ModElements.of(ModElements.ANEMO);
        GenshinElement attackerMain = ctx.attackerElement().getMainElement();
        boolean attackerIsAnemo = (attackerMain == anemoEl);

        GenshinElement spreadElement;
        GenshinElement defenderTarget;

        if (attackerIsAnemo) {
            spreadElement = findSpreadElement(ctx.targetContainer());
            if (spreadElement == null) {
                return ReactionResult.builder(type()).build();
            }
            defenderTarget = spreadElement;
        } else {
            defenderTarget = anemoEl;
            spreadElement = attackerMain;
        }

        float totalDefenderUnit = sumConsumable(ctx.targetContainer(), defenderTarget);
        if (totalDefenderUnit <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        float attackerQty = ctx.attackerUnit();

        float[] consumed = attackerIsAnemo
                ? calculateConsumption(totalDefenderUnit, attackerQty)
                : calculateConsumption(attackerQty, totalDefenderUnit);
        float consumedSpreadSide = consumed[0];
        float consumedAnemoSide = consumed[1];
        if (consumedSpreadSide <= 0f || consumedAnemoSide <= 0f) {
            return ReactionResult.builder(type()).build();
        }

        if (attackerIsAnemo) {
            consumeElementUnit(ctx.targetContainer(), defenderTarget, consumedSpreadSide);
            consumeElementUnit(ctx.targetContainer(), anemoEl, consumedAnemoSide);
        } else {
            consumeElementUnit(ctx.targetContainer(), defenderTarget, consumedAnemoSide);
            consumeElementUnit(ctx.targetContainer(), spreadElement, consumedSpreadSide);
        }

        LivingEntity target = ctx.targetEntity();
        if (target != null) {
            lastSwirlTick.put(target.getUUID(), target.level().getGameTime());
        }

        if (attackerIsAnemo && ModElements.is(spreadElement, ModElements.CYRO)
                && ElementLibApi.variantGate().stellarSwirl(ctx.attackerEntity(), target)) {
            handleStellarSwirl(ctx, target);
        } else {
            applySwirlDamage(ctx, spreadElement, target);
            spreadToNearby(ctx, spreadElement, calculateSpreadQuantity(consumedAnemoSide), target);
        }

        return ReactionResult.builder(type())
                .reacted()
                .consumedAttacker(attackerIsAnemo ? consumedAnemoSide : consumedSpreadSide)
                .consumedDefender(attackerIsAnemo ? consumedSpreadSide : consumedAnemoSide)
                .build();
    }

    private void handleStellarSwirl(ReactionContext ctx, @Nullable LivingEntity target) {
        if (target == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        if (ModEntities.STELLAR_VORTEX == null) return;

        Entity attacker = ctx.attackerEntity();
        ReactionDamage.dealDirect(ModReactionTypes.STELLAR_SWIRL_WIND.get(),
                ModElements.of(ModElements.ANEMO), attacker, target,
                ElementLibConfig.baseDamage("stellar_swirl_wind"));
        ReactionFeedback.reaction(target, ModReactionTypes.STELLAR_SWIRL_WIND.get(),
                ModElements.of(ModElements.ANEMO));

        float iceDamage = ElementLibConfig.baseDamage("stellar_swirl_ice");
        ElementalAreaEntity.spawn(level, ModEntities.STELLAR_VORTEX.get(), target.position(),
                attacker, StellarVortexEntity.RADIUS, StellarVortexEntity.DURATION_TICKS,
                iceDamage * STELLAR_VORTEX_TICK_DAMAGE_RATIO, iceDamage);
    }

    @Nullable
    private static GenshinElement resolveElement(String id) {
        if (PYRO_ID.equals(id)) return ModElements.of(ModElements.PYRO);
        if (HYDRO_ID.equals(id)) return ModElements.of(ModElements.HYDRO);
        if (ELECTRO_ID.equals(id)) return ModElements.of(ModElements.ELECTRO);
        if (CYRO_ID.equals(id)) return ModElements.of(ModElements.CYRO);
        if (ANEMO_ID.equals(id)) return ModElements.of(ModElements.ANEMO);
        if (FROZEN_ID.equals(id)) return ModElements.of(ModElements.FROZEN);
        return null;
    }

    private static boolean isSwirlable(GenshinElement element) {
        ResourceLocation key = ModRegistries.ELEMENT_REGISTRY.getKey(element);
        return key != null && SWIRLABLE_IDS.contains(key.toString());
    }

    @Nullable
    private GenshinElement findSpreadElement(StatusContainer container) {
        if (ElectroChargedReaction.findElement(container, ModElements.of(ModElements.FROZEN)) != null) {
            for (String id : SPREAD_PRIORITY_IDS) {
                GenshinElement elem = resolveElement(id);
                if (elem != null && ElectroChargedReaction.findElement(container, elem) != null) {
                    return elem;
                }
            }
            return resolveElement(CYRO_ID);
        }

        for (String id : SPREAD_PRIORITY_IDS) {
            GenshinElement elem = resolveElement(id);
            if (elem == null) continue;
            ElementalAttachmentInstance inst = ElectroChargedReaction.findElement(container, elem);
            if (inst != null && inst.getUnit() > 0) {
                return elem;
            }
        }
        return null;
    }

    private static float calculateSpreadQuantity(float consumedAnemoSide) {
        return consumedAnemoSide >= 2.0f ? STRONG_SWIRL_SPREAD : WEAK_SWIRL_SPREAD;
    }

    private void applySwirlDamage(ReactionContext ctx, GenshinElement spreadElement,
                                  @Nullable LivingEntity target) {
        if (target == null) return;
        ReactionDamage.dealDirect(ModReactionTypes.SWIRL.get(), spreadElement, ctx.attackerEntity(),
                target, ElementLibConfig.baseDamage("swirl"));
        ReactionFeedback.transformative(target, type(), spreadElement);
    }
    private void spreadToNearby(ReactionContext ctx, GenshinElement spreadElement,
                                float spreadQuantity, @Nullable LivingEntity target) {
        if (target == null) return;
        if (!(target.level() instanceof ServerLevel level)) return;
        double rSq = SWIRL_RADIUS * SWIRL_RADIUS;

        AttachmentProfile spreadProfile = createSpreadProfile(spreadQuantity);

        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(SWIRL_RADIUS),
                e -> e != target && e.isAlive() && target.distanceToSqr(e) <= rSq)) {

            ReactionFeedback.reaction(nearby, type(), spreadElement);

            StatusContainer nearbyContainer = ElementalAttachments.container(nearby);
            if (nearbyContainer == null) continue;

            EntityHost nearbyHost = EntityHost.of(nearby);

            // 拒收这次附着的目标不会跟着反应（与直接攻击同一条规则）。
            ElementalAttachmentHelper.attach(nearbyHost, spreadElement,
                    AttachmentSource.SPECIAL, spreadProfile,
                    AttachContext.reactionWrite(ctx.attackerEntity()));
        }
    }

    /** 传播附着：量越大衰减越慢。 */
    private static AttachmentProfile createSpreadProfile(float quantity) {
        float t = 7f + 2.5f * quantity;
        float v = quantity / t;
        return new AttachmentProfile(quantity, 1.0f, v, t);
    }
}