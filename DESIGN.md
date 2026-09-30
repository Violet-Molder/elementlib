# ElementLib 抽取设计契约（给实现者的唯一真相）

本文件描述 `elementlib/` 这个新 Gradle 子模块的**全部**约定。所有实现必须严格遵守，
否则最后一次性编译会失败。

## 0. 目标

把主模组 `minegenshin` 里的**元素体系**抽成一个**独立可运行**的库模块：

- 计时计数器（DecayCounter*）
- 衰减序列（只保留**元素附着量**序列，**删除**伤害序列与削韧序列）
- 元素注册（GenshinElement / ModElements / 元素注册表）
- 元素附着（AttachmentProfile / ElementalAttachmentHelper / ElementalAttachmentInstance / StatusContainer）
- 元素反应（ElementalReaction + Manager + 若干内置反应）
- 环境元素附着（方块元素体系 BlockElement* + 方块自附着 + 方块状态迁移）

**抽离掉与 `PGCharacter` 相关的一切**，以及伤害管线、削韧、角色效果、武器被动、实体（雷暴云/星璇）。

**源工程 `src/main/java/com/linweiyun/genshin/**` 只能读，绝对不许修改或删除。**

## 1. 坐标与命名

| 项 | 值 |
| --- | --- |
| 模块目录 | `elementlib/`（与根 `settings.gradle` 同级） |
| Gradle 工程路径 | `:elementlib` |
| Java 包根 | `com.linweiyun.elementlib` |
| mod id | `elementlib` |
| mod 显示名 | ElementLib |
| 主类 | `com.linweiyun.elementlib.ElementLib` |
| 资源命名空间 | `elementlib` |

包名映射统一为：

```
com.linweiyun.genshin.X  ->  com.linweiyun.elementlib.X
```

**类名、方法名、javadoc 尽量原样保留**（除了下面明确要求删改的部分）。这样抽取是机械的，
谁读源码都能对上。

## 2. 依赖边界

`elementlib` 的 `build.gradle` 只声明：`java-library`、`net.neoforged.moddev`、Minecraft/NeoForge、
以及 **LDLib2**（唯一外部前置，因为 `StatusContainer` 的多态序列化用了它的
`IPersistedSerializable` / `@Persisted` / `PersistedParser` / `Platform`）。

**禁止**在 elementlib 里出现：

- 任何 `com.linweiyun.genshin.*` 的 import（一个都不行）
- `com.lowdragmc.photon`、`com.geckolib`
- 除 LDLib2 的 `syncdata` / `utils.PersistedParser` / `Platform` / `LDLib2` 之外的 LDLib2 API

## 3. 被删除的类型与替代方案（唯一真相表）

| 主模组类型 | elementlib 处理 |
| --- | --- |
| `PGCharacter` | **删除**。需要「谁挂的」的地方一律改成 `String sourceKey`（可空）。 |
| `PlayerCharactersAttachment` / `AttachmentRegistration` | **删除**。用 `core/attachment/ElementalAttachments.java`（见 §4.1）。 |
| `ModDamageSpec` / `ModDamageSource` / `DamageIndicatorFactory` / `AttackType` | **删除**。伤害类表现改用 `core/system/reaction/ReactionFeedback`（见 §4.6）。`ReactionContext` / `AttachContext` 里不再有 `damageSpec` 字段。 |
| `StatusAccessor` / `CharacterChillHandler` / `ColdAura` 的角色分支 | **删除**。`ColdAura` 只处理生物实体。 |
| `ControlService` / `ControlRequest` | **删除**。`ColdElement.applyFreeze` 改成：`entity instanceof Mob mob -> mob.setNoAi(frozen)`。 |
| `HymnTheMaelstrom`（武器被动） | **删除**，相关调用整段删掉。 |
| `StellarVortexEntity` / `ModEntities` / 星扩散、星超导分支 | **删除**（连 `StellarGlimmer*` 一起不搬）。 |
| `ThunderCloudEntity` / `LunarChargedReaction` | **删除**（月感电不搬）。 |
| `ICharacterEffect` / `CharacterEffectHelper` / `ModCharacterEffects` / `ScarletProof*` / `VodyanitsaTalent` / `IStellarStateHolder` / `IStellarHousehold` / `IStellarHousehold.StellarHousehold` | **删除**。 |
| `ReactionPriorityCalculator.hasColumbinaInParty` / `hasStellarSwirlHousehold` / `hasStellarSwirlParticipant` / `getStellarStateHolders` / `clearSnapshots` | **删除**。保留 `computeFor` / `priorityOf` / `hasFrozen` / `hasAggravate` / `UNKNOWN_PRIORITY`。 |
| `TickSnapshot` / `HotPathLog` | **删除**。日志直接 `LOGGER.debug(...)` 或 `LOGGER.info(...)`。 |
| `TeyvatLiving`（`IDecayCounterHolder` 的父接口） | **删除**继承关系，`IDecayCounterHolder` 变成普通接口。 |
| `EntityHost` 里对 `ElementalAttachable` 的筛查 | **保留**（它只依赖元素接口，不依赖角色）。 |
| `LivingEntityElementalMixin` | **保留并搬过去**（默认全部接收）。 |
| `BlockElementHelper` 的 `checkPlayerEnvironment` / `checkAndApplyWaterToEntity` 里的角色逻辑 | 保留方法，但只做「实体/玩家身上挂环境元素」，删掉 `CharacterHost`、`StatusAccessor`、队伍角色相关调用。 |

## 4. 目标 API（elementlib 侧，必须逐字一致）

### 4.1 `core/attachment/ElementalAttachments.java`

替代主模组的 `AttachmentRegistration`，只留元素相关的两个附件：

```java
public final class ElementalAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementLib.MOD_ID);

    /** 生物身上的元素状态容器。 */
    public static final Supplier<AttachmentType<StatusContainer>> CONTAINER =
            ATTACHMENTS.register("status_container",
                    () -> AttachmentType.serializable(StatusContainer::new)
                            .sync(StatusContainer.STREAM_CODEC));

    /** 区块上的方块元素容器表。 */
    public static final Supplier<AttachmentType<ChunkBlockElements>> CHUNK_ELEMENTS =
            ATTACHMENTS.register("chunk_elements",
                    () -> AttachmentType.builder(ChunkBlockElements::new)
                            .serialize(ChunkBlockElements.CODEC.fieldOf("elements")));

    /** 计时计数器管理器（每实体一份，不落存档）。 */
    public static final Supplier<AttachmentType<DecayCounterManager>> DECAY_COUNTER =
            ATTACHMENTS.register("decay_counter",
                    () -> AttachmentType.builder(() -> null)
                            .build());

    public static void register(IEventBus modEventBus) { ATTACHMENTS.register(modEventBus); }

    /** 便捷取容器。 */
    public static StatusContainer container(LivingEntity entity) { ... }
    public static DecayCounterManager decayCounter(LivingEntity entity) { ... } // 懒建并 setData
}
```

注意 `AttachmentType.builder(() -> null).build()` 对 `DECAY_COUNTER` 是合法的（`DecayCounterManager`
不可序列化、不落存档）。`decayCounter(...)` 首次调用时 `computeIfAbsent` 语义：
`entity.getData(DECAY_COUNTER)` 为 null 就 `new DecayCounterManager(entity)` 再 `setData`。

### 4.2 `core/element/GenshinElement.java`

与源文件相同，**唯一改动**：`import com.linweiyun.genshin.core.system.registry.ModRegistries;`
-> `import com.linweiyun.elementlib.core.system.registry.ModRegistries;`，包名改掉。
`onAttach` / `onDetach` / `isNonPlayerLiving` 全部保留。

### 4.3 `core/element/ModElements.java` / `ColdElement.java`

- 包名改名；`ModRegistries.ELEMENTS` 指向 elementlib 自己的注册表。
- `ColdElement`：去掉 `ControlService` / `ControlRequest`，`applyFreeze` 用 `Mob#setNoAi`；
  `SLOW_MODIFIER_ID` 的命名空间改为 `elementlib:cryo_slow`。

### 4.4 `core/system/about/**`（附着）

- `AttachmentProfile` / `AttachmentSource` / `AttachmentType` / `AttachResult`：原样搬。
- `AttachContext`：把 `PGCharacter character` 换成 `String sourceKey`：

```java
public record AttachContext(@Nullable String sourceKey, long gameTime,
                            @Nullable Entity attackerEntity, @Nullable Float reactionUnit) {
    public static final AttachContext ENVIRONMENT = new AttachContext(null, 0L, null, null);
    public static AttachContext attack(@Nullable String sourceKey, long gameTime,
                                       @Nullable Entity attackerEntity, float reactionUnit) { ... }
    public static AttachContext reactionWrite(@Nullable Entity attackerEntity) { ... }
}
```

- `ElementalAttachmentInstance`：删掉 `PGCharacter sourceCharacter` 字段与 `setSourceCharacter/getSourceCharacter`；
  保留 `sourceCharacterKey`、`getFrozenCyroSourceKeys`、`refreshSource(String, long)`。
  `setContainer` / `setHost` / `getOwner` / `getDecayEndTick` 等全部保留。
- `ElementalAttachmentHelper`：
  - 删除所有带 `PGCharacter` 的重载；
  - 新增/改签名：

    ```java
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile);
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      AttachContext context);
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile,
                                      @Nullable String sourceKey, long gameTime);
    public static AttachResult attachInternal(...);
    public static AttachResult attachInternalTo(StatusContainer container, ElementalHost host, ...);
    public static AttachResult attach(LivingEntity target, StatusContainer container, ...);
    public static float consume(StatusContainer container, GenshinElement element, float amount);
    ```
  - `doAttach` 内部逻辑（覆盖规则、损耗、`canOverrideDecay`、按 `sourceKey` 匹配同源实例）**原样保留**，
    只是 `character` 变量换成 `sourceKey`（比较用 `String.equals`）。
  - 收尾 `finish(...)` 里构造 `ReactionContext` 时不传 damageSpec。
- `ElementalAttachable`：原样搬（`isImmuneToElementDamage` 保留，不依赖角色）。
- `ColdAura`：原样搬（只依赖实体）。
- `FrozenDecayState`：原样搬。
- `host/ElementalHost`：原样搬（`character()` 相关注释删掉）。
- `host/EntityHost`：容器取 `ElementalAttachments.CONTAINER`，其余原样。
- `host/BlockHost`：原样搬（容器走 `BlockElementStore`）。
- `host/CharacterHost`：**不搬**。

### 4.5 `core/system/reaction/**`

- `ElementalReaction`：原样搬（`resolveElement` 用 elementlib 的 `ModRegistries`）。
- `ElementalReactionType`：原样搬（枚举全留，未实现的反应类型不注册即可）。
- `ReactionResult` / `ReactionContext`：搬；`ReactionContext` 去掉 `damageSpec` 分量：

```java
public record ReactionContext(GenshinElement attackerElement, float attackerUnit,
                              AttachmentSource attackerSource, AttachmentProfile attackerProfile,
                              Entity attackerEntity,
                              StatusContainer targetContainer, LivingEntity targetEntity,
                              ElementalHost targetHost) { ... }
```

- `ElementalReactionManager`：原样搬，凡 `DamageIndicatorFactory.*` 改成
  `ReactionFeedback.*`，`runReactions` 里 `context.damageSpec()` 相关行删掉。
- `ReactionPriorityCalculator`：只保留 §3 表里列出的部分。
- `ElectroChargedTickState`：搬，`dealDamage`/`chainNearbyWet` 改成调 `ReactionFeedback.transformative(...)`；
  删掉 `hasColumbinaInParty` 判定；保留 `container` / `onActiveTrigger` / `onTick` / `tryAutoActivate` 结构。
- `ReactionFeedback`（**新类**）：见 §4.6。
- `StellarGlimmer` / `StellarGlimmerBranch`：**不搬**。

### 4.6 `core/system/reaction/ReactionFeedback.java`（新类，全服务端）

```java
public final class ReactionFeedback {
    /** 元素颜色粒子（按元素选 ParticleTypes）+ 附近玩家 action bar 文案。 */
    public static void reaction(@Nullable LivingEntity target, ElementalReactionType type,
                                @Nullable GenshinElement element);
    /** 剧变反应（超导 / 感电 / 扩散）：粒子 + action bar + 播放一个原版音效。 */
    public static void transformative(@Nullable LivingEntity target, ElementalReactionType type,
                                      GenshinElement element);
    public static void atBlock(ServerLevel level, BlockPos pos, ElementalReactionType type);
    public static int colorOf(@Nullable GenshinElement element); // 0xRRGGBB
}
```

实现要求：只在 `level instanceof ServerLevel` 时发粒子和消息（客户端调用直接返回）；
消息用 `Component.translatable(type.getDisplayName())` 或 `Component.literal("[" + type + "]")`，
发给目标 12 格内的 `ServerPlayer`（`displayClientMessage(msg, true)`）。
元素 → 粒子映射：

```
FYSIKOS/GEO -> ParticleTypes.CRIT
PYRO        -> ParticleTypes.FLAME
HYDRO       -> ParticleTypes.SPLASH
ELECTRO     -> ParticleTypes.ELECTRIC_SPARK
CYRO/FROZEN/COLD -> ParticleTypes.SNOWFLAKE
ANEMO       -> ParticleTypes.CLOUD
DENDRO      -> ParticleTypes.HAPPY_VILLAGER
其他         -> ParticleTypes.END_ROD
```

### 4.7 `core/system/reaction/builtin/**`（要搬的 6 个反应）

| 类 | 改动 |
| --- | --- |
| `VaporizeReaction` | 只改包名 + `ReactionConfig`（elementlib 自己的）。 |
| `MeltReaction` | 同上；`applyHostEffect` 里的方块化水逻辑保留（依赖 `BlockHost`/`BlockElementStore`/`BlockElementRules`）。 |
| `FreezeReaction` | 去掉 `HymnTheMaelstrom` 整段、去掉 `getSourceCharacter`（只用 `getSourceCharacterKey`）；其余保留。 |
| `SuperConductReaction` | 去掉伤害管线；`applyDamageOffCooldown` 改成 `ReactionFeedback.transformative(target, SUPERCONDUCT, CYRO)` 并保留 10 tick 冷却 + `BoundedLruMap`。 |
| `ElectroChargedReaction` | 去掉 `hasColumbinaInParty` 判定，其余保留。 |
| `SwirlReaction` | 只保留「扩散本体」：`canMatch`（风 vs 可扩散元素）、冷却、消耗、`applySwirlDamage`→`ReactionFeedback.transformative`、`spreadToNearby`（走 `ElementalAttachmentHelper.attach`）。**删除** 星扩散/星超导、`StellarVortexEntity`、`VodyanitsaTalent`、`HymnTheMaelstrom`、`ScarletProof`、`resolveTriggerCharacter`、`handleStellarSwirl`、`hasHiddenSwirlable`、队伍贡献者相关代码。 |

注册文件 `core/system/registry/register/ModElementalReactions.java` 只注册
`VAPORIZE / MELT / FREEZE / SWIRL / ELECTRO_CHARGED / SUPERCONDUCT` 六个，
元素 id 字符串用 `elementlib:xxx`。

### 4.8 `core/system/registry/**`

- `ModRegistries`：只保留 `ELEMENT_REGISTRY` / `ELEMENTAL_REACTIONS_REGISTRY` /
  `STATUS_INSTANCE_TYPE_REGISTRY`（以及它们的 DeferredRegister）。命名空间用 `elementlib`。
  删除 `ATTRIBUTE_TYPE` / `CHARACTER` / `CHARACTER_EFFECT` / `ARTIFACT_SET` 及其 DeferredRegister，
  删除 `ICharacterEffect` / `PGCharacter` / `ArtifactSet` / `AttributeType` import。
- `register/ModStatusInstanceTypes`（如果主模组有）：搬成只注册 `elemental_attachment` 一个类型。

### 4.9 `core/status/**`

- `StatusInstance` / `StatusInstanceType` / `StatusInstanceTypes`：原样搬；
  `StatusInstanceTypes.create` 用 `ElementLib.id(typeId)` 兜底。
- `StatusContainer`：搬到 `core/attachment/StatusContainer.java`（保持包 `core.attachment`）。
  **删除** `getActiveContributors` / `getLastAttacher` / `resolveCharacterByKey`（全部角色相关），
  其余（含 `tick()`、`add/remove/clear/find/hasAlive/copy`、NBT/网络多态序列化）原样保留。
  `AttachmentRegistration.CONTAINER` -> `ElementalAttachments.CONTAINER`。
- `StatusTickHandler`：搬到 `event/StatusTickHandler.java`，删掉 `PoiseFreezeBreak` 调用
  （削韧不属于本库），保留容器 tick、`ColdAura.tick`、`freezeMotion`。

### 4.10 `core/system/combat/decay/**`（计时计数器 + 只含元素量的衰减序列）

- `DecaySequence`：从 `core/system/combat/damage/DecaySequence.java` 搬过来，
  **只保留元素量相关常量与 API**（`DEFAULT_ELEMENT`、`createFilled`、`createRepeating`、`getCoefficient`、
  构造与缓存逻辑）；`DEFAULT_DAMAGE` / `DEFAULT_POISE` 删除。
- `DecayGroup`：改成只含两个分量：

```java
public record DecayGroup(int clearTimeTicks, DecaySequence elementSequence) {
    public static final int DEFAULT_CLEAR_TIME_TICKS = 50;
    public float getElementCoefficient(int hitCount) { ... }
}
```

- `DecayGroups`：只保留 `DEFAULT_NORMAL_ATTACK` / `DEFAULT_ELEMENTAL_SKILL` /
  `DEFAULT_ELEMENTAL_BURST`；`SHENHE_SKILL` 删掉（角色专属）。
- `DecayResult`：只保留 `elementCoefficient` 与 `hasElementAttachment()`；删掉 damage/poise。
- `DecaySpec`（**新类**）：替代 `ModDamageSpec` 在计数器里的作用：

```java
public record DecaySpec(String decayTag, DecayGroup group) {}
```

- `DecayCounterData`：删掉 `getDamageCoefficient` / `getPoiseCoefficient`。
- `DecayCounterManager`：签名改成

```java
public DecayResult processHit(LivingEntity attacker, @Nullable String characterKey,
                              DecaySpec spec, long currentTick);
public DecayCounterData getOrCreateCounter(LivingEntity attacker, @Nullable String characterKey,
                                           DecaySpec spec, long currentTick);
```

  key 构造规则原样（`attackerUuid : characterKey/直接 : decayTag : groupId`）。
- `DecayCounterWorker` / `DecayCounterService` / `IDecayCounterHolder`：原样搬，
  `IDecayCounterHolder` 不再 extends `TeyvatLiving`。

### 4.11 `core/system/about/block/**`（环境元素附着）

7 个类全部搬：`BlockElementHelper` / `BlockElementMigrations` / `BlockElementRules` /
`BlockElementStore` / `BlockElementTicker` / `BlockSelfAura` / `ChunkBlockElements`。

- `AttachmentRegistration.CHUNK_ELEMENTS` -> `ElementalAttachments.CHUNK_ELEMENTS`。
- `BlockElementHelper`：删掉 `PlayerCharactersAttachment` / `PGCharacter` / `CharacterHost` /
  `StatusAccessor` 相关分支。保留 `applyElement(ServerLevel, BlockPos, ...)`、
  `onServerTick(ServerLevel)`、`trackFrozen` / `untrackFrozen` / `trackedTick` /
  `tickBlockElementDecay` / `checkAndApplyWaterToEntity` / `checkPlayerEnvironment`
  的**环境元素附着行为**（走到实体/玩家身上的元素依旧通过 `ElementalAttachmentHelper.attach`）。
- `BlockElementTicker`：原样搬（LevelTickEvent / ChunkEvent）。

### 4.12 `util/log/**` 与 `core/system/performance/BoundedLruMap`

- `util/log/LogGroup` / `util/log/ModLog`：原样搬到 elementlib（总开关改成 `ElementLib.LOG_ENABLED`）。
- `core/system/performance/BoundedLruMap`：原样搬到 elementlib。

### 4.13 `config/reaction/ReactionConfig.java`

elementlib 自己的极简版本，只保留反应倍率，用标准 NeoForge `ModConfigSpec.DoubleValue`：

```java
public class ReactionConfig {
    public static ModConfigSpec.DoubleValue MELT;
    public static ModConfigSpec.DoubleValue MELT_NEGATIVE;
    public static ModConfigSpec.DoubleValue VAPORIZE;
    public static ModConfigSpec.DoubleValue VAPORIZE_NEGATIVE;
    public static ModConfigSpec.DoubleValue SUPERCONDUCT;
    public static ModConfigSpec.DoubleValue SWIRL;
    public static ModConfigSpec.DoubleValue ELECTROCHARGED;

    public static void register(ModConfigSpec.Builder builder) { ... }
}
```

调用点写法 `(float) ReactionConfig.MELT.get()`（`DoubleValue#get()` 返回 `Double`）。

### 4.14 测试入口（必须做，游戏内可验收）

`command/ElementLibCommand.java`，注册到 NeoForge 命令事件，根命令 `/elementlib`：

```
/elementlib attach <element> [amount] [source]   # 给「看向的实体，没有就自己」挂元素，默认 1U / NORMAL_ATTACK
/elementlib clear [entity]                       # 清空目标容器
/elementlib info [entity]                        # 列出容器里所有附着（元素 / 量 / 衰减速率 / 剩余 tick）
/elementlib decay <tag> [hits]                   # 用 DEFAULT_* 组跑一次计数器，打印元素量系数序列
/elementlib block <element> [amount]             # 给脚下（或看向的）方块挂元素，测环境附着/迁移
/elementlib reactions                            # 打印已注册的反应清单
```

- 命令单独放在 `command` 包，`@EventBusSubscriber` 监听 `RegisterCommandsEvent`。
- 反馈用 `context.getSource().sendSuccess(...)` / `sendFailure(...)`。
- `/elementlib attach` 必须**真的走** `ElementalAttachmentHelper.attach(...)`，这样反应会自然触发。

### 4.15 `ElementLib` 主类 + 事件

`ElementLib.java`：

```java
@Mod(ElementLib.MOD_ID)
public class ElementLib {
    public static final String MOD_ID = "elementlib";
    public static volatile boolean LOG_ENABLED = true;
    public static final Logger LOGGER = ...;
    public ElementLib(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, ElementLibConfig.SPEC, "elementlib/reaction.toml");
        ModElements.register(modEventBus);
        ModElementalReactions.register(modEventBus);
        ModStatusInstanceTypes.register(modEventBus);
        ElementalAttachments.register(modEventBus);
    }
    private void commonSetup(FMLCommonSetupEvent e) { ModElements.setupSubElements(); }
    public static Identifier id(String path) { ... }
    @SubscribeEvent public void onServerStarting(ServerStartingEvent e) { DecayCounterService.initOnServer(...); }
    @SubscribeEvent public void onServerTick(ServerTickEvent.Post e) { for (ServerLevel l : ...) BlockElementHelper.onServerTick(l); }
    @SubscribeEvent public void onServerStopping(ServerStoppingEvent e) { DecayCounterService.shutdown(); }
}
```

`ElementLibConfig`：持有 `ModConfigSpec SPEC` 与 `ReactionConfig` 的 `register(builder)` 调用。

## 5. 硬性禁止

1. 不许改 `src/**` 下任何主模组文件（除了 `settings.gradle` / 根 `build.gradle` 由我统一改）。
2. 不许 import `com.linweiyun.genshin.*`。
3. 不许 `new` 出未在本文档出现的类。
4. 不许改动本文档已固定的公开签名。
5. 每个 `.java` 文件顶部必须有 `package com.linweiyun.elementlib....;`。
