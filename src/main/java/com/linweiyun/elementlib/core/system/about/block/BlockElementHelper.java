package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.api.ElementLibApi;
import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.api.EnvironmentAttachTarget;
import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.system.about.AttachResult;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 方块元素行为 —— 「宿主适配 + 反应结果驱动的状态迁移」这一层。
 * 取不到元素就跳过。
 */
public final class BlockElementHelper {

    /** 本 game tick 已经吃过某个元素的格子；防止同一刻多条入口重复附着。 */
    private static final Map<String, Long> APPLIED_THIS_TICK = new HashMap<>();
    private static long appliedTick = Long.MIN_VALUE;

    private static boolean alreadyApplied(ServerLevel level, BlockPos pos, GenshinElement element) {
        long now = level.getGameTime();
        if (now != appliedTick) {
            APPLIED_THIS_TICK.clear();
            appliedTick = now;
        }
        String key = level.dimension().identifier() + "@" + pos.asLong() + "#" + element.getId();
        Long last = APPLIED_THIS_TICK.get(key);
        if (last != null && last == now) {
            return true;
        }
        APPLIED_THIS_TICK.put(key, now);
        return false;
    }

    public static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    private static final int WATER_CHECK_INTERVAL = 20;
    private static int waterEntityCheckCounter = 0;

    private BlockElementHelper() {
    }

    // ==================== 方块附着入口 ====================

    /**
     * 对方块附着元素 —— 唯一入口。
     *
     * @param gauge       附着量（U），作为这条附着的初始量（方块侧无损耗）
     * @param decayPerSec 衰减率（U/s）
     */
    public static void applyElement(ServerLevel level, BlockPos pos,
                                    GenshinElement element,
                                    float gauge, float decayPerSec) {
        applyElementAndGet(level, pos, element, gauge, decayPerSec);
    }

    /** 与 {@link #applyElement} 同一套行为（含落盘与方块表现迁移），返回这次附着的结果。 */
    public static AttachResult applyElementAndGet(ServerLevel level, BlockPos pos,
                                                  GenshinElement element,
                                                  float gauge, float decayPerSec) {
        if (level == null || pos == null || element == null || gauge <= 0f) {
            return AttachResult.REJECTED;
        }
        BlockHost host = BlockHost.of(level, pos);
        if (host == null || !host.isValid()) {
            return AttachResult.REJECTED;
        }

        // 零分配预筛：这个方块对这个元素压根不感兴趣 → 连容器都不建、不落盘。
        if (!BlockElementRules.accepts(host.state(), element)) {
            return AttachResult.REJECTED;
        }
        if (alreadyApplied(level, pos, element)) {
            return AttachResult.REJECTED;
        }

        StatusContainer container = host.container();
        if (container == null) {
            return AttachResult.REJECTED;
        }

        AttachmentProfile profile = new AttachmentProfile(gauge, 1.0f, decayPerSec, 999f);

        // 附着 —— 入口内部会接着尝试反应（与实体端同一套）
        AttachResult result = ElementalAttachmentHelper.attach(
                host, element, AttachmentSource.ENVIRONMENTAL, profile);
        if (!result.attached()) {
            // 没挂上就什么都不落：不提交、也不跑迁移（否则「冰族没冰就化水」的规则会把误触当融化）
            return result;
        }

        // 落盘 + 让方块状态跟上
        host.commit(container);
        migrateBlockState(host, container);
        return result;
    }

    /** 方块表现迁移 —— 交给 {@link BlockElementMigrations} 注册表。 */
    private static void migrateBlockState(BlockHost host, StatusContainer container) {
        BlockElementMigrations.runAll(host, container);
    }

    // ==================== 水环境给实体挂水 ====================

    /** 水/雨环境附着 —— 给实体自己挂弱水。 */
    public static void checkAndApplyWaterToEntity(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        boolean raining = entity.level() instanceof ServerLevel level && level.isRaining();
        checkAndApplyWaterToEntity(entity, raining);
    }

    /**
     * 带上「当前维度是否在下雨」的重载（{@link #onServerTick} 的批量扫描用）。
     */
    private static void checkAndApplyWaterToEntity(LivingEntity entity, boolean raining) {
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (entity.isSpectator()) return;

        GenshinElement hydro = ElementRoles.of(ElementRoles.HYDRO);
        GenshinElement pyro = ElementRoles.of(ElementRoles.PYRO);

        // 玩家：水（含淋雨）与火都是环境元素源，落点由环境宿主出口决定
        if (entity instanceof Player player) {
            ElementalHost host = environmentHost(player);
            if (host == null) return;
            if (hydro != null && (player.isInWater() || (raining && isRainedOn(level, player)))) {
                ElementalAttachmentHelper.attach(host, hydro,
                        AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
            }
            if (pyro != null && isInFire(player)) {
                ElementalAttachmentHelper.attach(host, pyro,
                        AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
            }
            return;
        }

        if (hydro == null) return;

        boolean inWater = entity.isInWater();
        if (!inWater) {
            if (!raining) return;
            if (!isRainedOn(level, entity)) return;
        }

        MobCategory cat = entity.getType().getCategory();
        if (cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT) return;

        ElementalHost host = environmentHost(entity);
        if (host == null) return;
        ElementalAttachmentHelper.attach(host, hydro,
                AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
    }

    /** 环境附着的落点；接入方未替换时就是实体自己。 */
    @Nullable
    private static ElementalHost environmentHost(LivingEntity entity) {
        EnvironmentAttachTarget target = ElementLibApi.environmentTarget();
        return target == null ? EntityHost.of(entity) : target.targetFor(entity);
    }

    /** 这个位置是不是正被雨淋着（生物群系在下雨 + 头顶见天）。 */
    private static boolean isRainedOn(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        return level.isRainingAt(pos) && level.canSeeSky(pos);
    }

    /**
     * 玩家专用的环境附着检查 —— 每 tick 跑一次，负责「刚进水 / 刚踩进火」那一刻就挂上元素。
     */
    public static void checkPlayerEnvironment(Player player) {
        if (player == null || player.isSpectator()) return;

        boolean inWater = player.isInWater();
        boolean inFire = isInFire(player);
        if (!inWater && !inFire) return;

        ElementalHost host = environmentHost(player);
        if (host == null) return;
        StatusContainer container = host.container();
        if (container == null) return;

        GenshinElement hydro = ElementRoles.of(ElementRoles.HYDRO);
        GenshinElement pyro = ElementRoles.of(ElementRoles.PYRO);

        if (hydro != null && inWater && !hasElement(container, ElementRoles.HYDRO)) {
            ElementalAttachmentHelper.attach(host, hydro,
                    AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
        }
        if (pyro != null && inFire && !hasElement(container, ElementRoles.PYRO)) {
            ElementalAttachmentHelper.attach(host, pyro,
                    AttachmentSource.ENVIRONMENTAL, AttachmentProfile.WEAK);
        }
    }

    /** 站在火里：岩浆里、身上烧着，或者脚下方块就是火 / 灵魂火。 */
    private static boolean isInFire(Player player) {
        if (player.isInLava() || player.isOnFire()) {
            return true;
        }
        BlockState feet = player.level().getBlockState(player.blockPosition());
        return feet.is(Blocks.FIRE) || feet.is(Blocks.SOUL_FIRE);
    }

    /** 环境附着推进。 */
    public static void onServerTick(ServerLevel level) {
        if (level == null) return;

        // 玩家每 tick 单独过一遍：进水 / 进火要立刻附着，不能等下一轮 1 秒的批量扫描
        for (ServerPlayer player : level.players()) {
            checkPlayerEnvironment(player);
        }

        if (++waterEntityCheckCounter < WATER_CHECK_INTERVAL) return;
        waterEntityCheckCounter = 0;

        boolean raining = level.isRaining();
        for (Entity e : level.getEntities().getAll()) {
            if (e instanceof LivingEntity living) {
                checkAndApplyWaterToEntity(living, raining);
            }
        }
    }

    // ==================== 冻结位置登记（集中式推进用） ====================

    /** 当前冻结着、需要推进衰减的方块位置（每个维度一份）。 */
    private static final Map<ResourceKey<Level>, Set<Long>> FROZEN_SITES = new ConcurrentHashMap<>();

    /** 登记一个需要推进的冻结方块。 */
    public static void trackFrozen(ServerLevel level, BlockPos pos) {
        FROZEN_SITES.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet())
                .add(pos.asLong());
    }

    /** 取消登记（融化/清空时）。 */
    public static void untrackFrozen(ServerLevel level, BlockPos pos) {
        Set<Long> set = FROZEN_SITES.get(level.dimension());
        if (set != null) {
            set.remove(pos.asLong());
        }
    }

    /** 每 tick 最多推进多少格：大范围冻结时不让单 tick 一次性跑完整张表。 */
    private static final int TRACK_LIMIT_PER_TICK = 256;

    /**
     * 每一 tick 统一推进登记在案的冻结方块。
     */
    public static void trackedTick(ServerLevel level) {
        Set<Long> set = FROZEN_SITES.get(level.dimension());
        if (set == null || set.isEmpty()) {
            return;
        }
        int processed = 0;
        Iterator<Long> it = set.iterator();
        while (it.hasNext() && processed < TRACK_LIMIT_PER_TICK) {
            BlockPos pos = BlockPos.of(it.next());
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                it.remove();
                continue;
            }
            tickBlockElementDecay(level, pos);
            processed++;
        }
    }

    // ==================== 方块元素推进 ====================

    /**
     * 推进这个方块的元素容器：跑衰减、跑容器自己的动态状态（冻元素衰减率）。
     */
    public static void tickBlockElementDecay(ServerLevel level, BlockPos pos) {
        // 只读路径先看一眼：没有元素数据的方块不归我们管，顺手从推进表里摘掉
        StatusContainer container = BlockElementStore.peek(level, pos);
        if (container == null) {
            untrackFrozen(level, pos);
            return;
        }
        // 同一 game tick 内只推进一次：身上挂了几条排期都无所谓
        if (!BlockElementStore.beginDecayStep(level, pos)) {
            return;
        }
        // 自愈：只要还冻着就保证它在推进表里（覆盖重启后丢表的情况）
        if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
            trackFrozen(level, pos);
        }

        container.tick();

        BlockElementMigrations.runAll(BlockHost.of(level, pos), container);

        if (container.isEmpty()) {
            BlockElementStore.clear(level, pos);
            untrackFrozen(level, pos);
        }
        // 否则不 commit：容器是原地改的对象，存档时自然带上；每 tick 提交会同步整 chunk 元素表。
    }

    // ==================== 内部：容器查询 ====================

    private static boolean hasElement(StatusContainer container, String role) {
        GenshinElement target = ElementRoles.of(role);
        return target != null && sumElementQuantity(container, target) > 0f;
    }

    private static float sumElementQuantity(StatusContainer container, GenshinElement target) {
        float sum = 0f;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ea.getElement() == target) {
                sum += ea.getUnit();
            }
        }
        return sum;
    }
}
