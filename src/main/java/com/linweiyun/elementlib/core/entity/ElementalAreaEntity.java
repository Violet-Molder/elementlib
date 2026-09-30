package com.linweiyun.elementlib.core.entity;

import com.linweiyun.elementlib.api.ElementalDamageReason;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.reaction.damage.ReactionDamage;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 元素区域实体：在半径内按固定间隔结算伤害，寿命到期时爆炸一次并销毁。
 *
 * <p>逻辑只在服务端执行；可见性由 {@code sendParticles} 提供，客户端不需要模型。
 */
public abstract class ElementalAreaEntity extends Entity {

    public static final double DEFAULT_RADIUS = 3.0D;
    public static final int DEFAULT_DURATION_TICKS = 100;
    public static final int DEFAULT_TICK_INTERVAL = 20;

    private static final int AMBIENT_PARTICLES = 4;
    private static final int EXPLODE_PARTICLES = 24;

    @Nullable
    private Entity owner;
    @Nullable
    private UUID ownerUUID;

    private double radius = DEFAULT_RADIUS;
    private int durationTicks = DEFAULT_DURATION_TICKS;
    private int tickInterval = DEFAULT_TICK_INTERVAL;
    private float tickDamage;
    private float explodeDamage;

    /** 到期时刻（绝对游戏时间）；{@code -1} = 尚未起算，第一次 tick 时定死。 */
    private long expireGameTime = -1L;
    private int tickTimer;

    protected ElementalAreaEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /**
     * 在 {@code pos} 生成一个区域实体并写入全部参数。
     *
     * @return 生成的服务端实体；客户端或非 {@link ServerLevel} 时返回 {@code null}
     */
    @Nullable
    public static <T extends ElementalAreaEntity> T spawn(Level level, EntityType<T> type, Vec3 pos,
                                                          @Nullable Entity owner, double radius,
                                                          int durationTicks, float tickDamage,
                                                          float explodeDamage) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        T entity = type.create(serverLevel, EntitySpawnReason.EVENT);
        if (entity == null) {
            return null;
        }
        entity.setPos(pos.x, pos.y, pos.z);
        entity.setOwner(owner);
        entity.setRadius(radius);
        entity.setDurationTicks(durationTicks);
        entity.setTickDamage(tickDamage);
        entity.setExplodeDamage(explodeDamage);
        serverLevel.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        long now = serverLevel.getGameTime();
        if (this.expireGameTime < 0L) {
            this.expireGameTime = now + this.durationTicks;
        }

        sendAreaParticles(serverLevel, AMBIENT_PARTICLES, 0.01D);

        if (now >= this.expireGameTime) {
            explode(serverLevel);
            return;
        }

        this.tickTimer++;
        if (this.tickInterval > 0 && this.tickTimer % this.tickInterval == 0) {
            ReactionDamage.dealAt(serverLevel, tickReactionType(), element(), ownerEntity(),
                    position(), this.radius, this.tickDamage, ElementalDamageReason.AREA_TICK);
        }
    }

    /** 结算一次爆炸伤害、放一次粒子，然后销毁自己。 */
    protected void explode(ServerLevel level) {
        ReactionDamage.dealAt(level, explodeReactionType(), element(), ownerEntity(),
                position(), this.radius, this.explodeDamage, ElementalDamageReason.AREA_EXPLODE);
        sendAreaParticles(level, EXPLODE_PARTICLES, 0.05D);
        this.discard();
    }

    private void sendAreaParticles(ServerLevel level, int count, double speed) {
        if (this.radius <= 0.0D) {
            return;
        }
        double spread = this.radius * 0.5D;
        level.sendParticles(areaParticle(),
                this.getX(), this.getY() + 0.25D, this.getZ(),
                count, spread, 0.4D, spread, speed);
    }

    /** 区域粒子的外观，按元素选择。 */
    protected ParticleOptions areaParticle() {
        GenshinElement e = element();
        String id = e == null ? null : e.getId();
        if (id == null) {
            return ParticleTypes.END_ROD;
        }
        return switch (id) {
            case "pyro", "burning" -> ParticleTypes.FLAME;
            case "hydro" -> ParticleTypes.SPLASH;
            case "electro", "aggravate" -> ParticleTypes.ELECTRIC_SPARK;
            case "cyro", "frozen", "cold" -> ParticleTypes.SNOWFLAKE;
            case "anemo" -> ParticleTypes.CLOUD;
            case "dendro", "wood" -> ParticleTypes.HAPPY_VILLAGER;
            default -> ParticleTypes.END_ROD;
        };
    }

    /** 周期结算使用的反应类型。 */
    protected abstract ElementalReactionType tickReactionType();

    /** 到期爆炸使用的反应类型。 */
    protected abstract ElementalReactionType explodeReactionType();

    /** 本区域绑定的元素；元素未注册时为 {@code null}。 */
    @Nullable
    protected abstract GenshinElement element();

    @Nullable
    public Entity ownerEntity() {
        if (this.owner == null && this.ownerUUID != null
                && this.level() instanceof ServerLevel serverLevel) {
            this.owner = serverLevel.getServer().getPlayerList().getPlayer(this.ownerUUID);
        }
        return this.owner;
    }

    public void setOwner(@Nullable Entity owner) {
        this.owner = owner;
        this.ownerUUID = owner == null ? null : owner.getUUID();
    }

    public double getRadius() {
        return this.radius;
    }

    public void setRadius(double radius) {
        this.radius = Math.max(0.0D, radius);
    }

    public int getDurationTicks() {
        return this.durationTicks;
    }

    public void setDurationTicks(int ticks) {
        this.durationTicks = Math.max(1, ticks);
        this.expireGameTime = -1L;
    }

    public int getTickInterval() {
        return this.tickInterval;
    }

    public void setTickInterval(int ticks) {
        this.tickInterval = Math.max(1, ticks);
    }

    public float getTickDamage() {
        return this.tickDamage;
    }

    public void setTickDamage(float damage) {
        this.tickDamage = Math.max(0.0F, damage);
    }

    public float getExplodeDamage() {
        return this.explodeDamage;
    }

    public void setExplodeDamage(float damage) {
        this.explodeDamage = Math.max(0.0F, damage);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        this.radius = input.getDoubleOr("el_radius", this.radius);
        this.durationTicks = input.getIntOr("el_duration", this.durationTicks);
        this.tickInterval = input.getIntOr("el_tick_interval", this.tickInterval);
        this.tickDamage = input.getFloatOr("el_tick_damage", this.tickDamage);
        this.explodeDamage = input.getFloatOr("el_explode_damage", this.explodeDamage);
        this.expireGameTime = input.getLongOr("el_expire", -1L);
        this.tickTimer = input.getIntOr("el_tick_timer", 0);

        // 引用不落盘，只留 UUID，重载后按 UUID 重新解析。
        this.owner = null;
        String ownerId = input.getStringOr("el_owner", "");
        if (ownerId.isEmpty()) {
            this.ownerUUID = null;
        } else {
            try {
                this.ownerUUID = UUID.fromString(ownerId);
            } catch (IllegalArgumentException ignored) {
                this.ownerUUID = null;
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("el_radius", this.radius);
        output.putInt("el_duration", this.durationTicks);
        output.putInt("el_tick_interval", this.tickInterval);
        output.putFloat("el_tick_damage", this.tickDamage);
        output.putFloat("el_explode_damage", this.explodeDamage);
        output.putLong("el_expire", this.expireGameTime);
        output.putInt("el_tick_timer", this.tickTimer);
        if (this.ownerUUID != null) {
            output.putString("el_owner", this.ownerUUID.toString());
        }
    }
}
