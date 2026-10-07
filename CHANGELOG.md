# 更新日志

> 版本命名自 26.2.0.3 起为「MC 版本 + mod 版本」（旧命名 1.0.3 = 26.2.0.3）。
> 1.21.1 分支自 **21.1.0.3** 起同样改用这套命名（旧命名 1.0.2 = 21.1.0.2）。
>
> **状态标记**：每个版本标注「已发布」或「当前更改」。**当前更改版本在正式发布前不提升版本号** ——
> 换会话、换窗口、继续加功能都挂在同一个版本号下面；要区分改动批次用日期与条目分段表达。
> 「已发布」的判定依据：Maven Central <https://repo1.maven.org/maven2/com/linweiyun/elementlib/> 下有没有该版本目录。

## 已发布版本

| 坐标 | 版本 | 状态 |
| --- | --- | --- |
| `elementlib-neoforge-26.2` | 26.2.0.3 | 已发布（Central 目录存在） |
| `elementlib-neoforge-26.2` | 26.2.0.2 | 已发布（Central 目录存在） |
| `elementlib-neoforge-1.21.1` | 1.0.2 | 已发布（Central 目录存在） |
| `elementlib-neoforge-1.21.1` | 1.0.1 | 已发布（Central 目录存在） |

---

## 21.1.0.3 — 当前更改（未发布）

把 26.2 分支的两批工作整批移植到 1.21.1：一、元素模块统一系统；二、事件网络。
两批内容的说明见下方 `26.2.0.4` 一节，本节的差异只在 **1.21.1 的 API 适配**。

### 1.21.1 适配差异

**存档序列化**

- LDLib2 1.21.1 的 `IPersistedSerializable` 是 `INBTSerializable<CompoundTag>`，所以
  `ElibModuleContainer` / `ChunkModules` 的存档路径用 `serializeNBT(Provider)` / `deserializeNBT(Provider, CompoundTag)`，
  不是 26.2 的 `ValueOutput` / `ValueInput`（1.21.1 没有这两个类）；
- NBT 取值口径随 1.21.1：`CompoundTag#getString/getLong` 直接返回原始值、`ListTag#getCompound(i)`、
  `CompoundTag#getList(key, Tag.TAG_COMPOUND)`；26.2 那边的 `orElse(...)` / `getCompoundOrEmpty(...)` 在这里不存在。

**类名与方法名**

- `Identifier` → `ResourceLocation`；`ResourceKey#identifier()` → `location()`；
- `Registry#getValue(...)` → `get(...)`；`FriendlyByteBuf#writeIdentifier/readIdentifier` →
  `writeResourceLocation/readResourceLocation`；
- `EntityTypes` → `EntityType`；`Player#isWithinBlockInteractionRange(...)` → `canInteractWithBlock(...)`。

**保持 1.21.1 原有实现**

- 附着图标渲染仍是 `RenderLevelStageEvent` + `MultiBufferSource`（26.2 换成了 `SubmitCustomGeometryEvent` + `SubmitNodeCollector`）；
- 元素剑仍是 `SwordItem`（1.21.1 有 `Tier` / `ToolMaterial`），只有命中附着改走统一攻击入口
  `ElibAttackPipeline`，与 26.2 口径一致；
- `gradle.properties` 仍是 1.21.1 那一套（MC 1.21.1 / NeoForge 21.1.216 / LDLib2 2.2.41 / Java 21 toolchain），
  只是 `mod_version` 抬到 `21.1.0.3`；发布用的 `maven-publish` + `signing` 与 artifactId
  `elementlib-neoforge-1.21.1` 随 26.2 分支一起接入。

---

## 26.2.0.4 — 当前更改（未发布）

本版包含两块工作。**未发布前不改版本号**，后续改动继续追加在本节。

### 一、元素模块统一系统

新增通用模块层与统一攻击入口；元素成为其中一个模块。本版为兼容版本，旧存档仍可读，
计划在下一个发布版本（预留 `26.2.0.5`）删除旧附件读取（见 `TODO-兼容清单.md`）。

**新增**

- 模块层：`ElibModuleType` / `ElibModuleData` / `ElibModuleContainer` / `ElibModuleHost` /
  `ElibModuleHosts` / `ElibModuleQuery` / `ElibModuleRegistry` / `ElibModuleTargetKinds`；
- 存储：实体附件 `elementlib:modules`、区块 `elementlib:chunk_modules`（持久区 + 瞬态区）、
  物品组件 `elementlib:item_modules`（只读）；外部宿主可用 `ElibModuleHosts.registerCarrier` 注册；
- 攻击：`ElibAttackAction` / `ElibAttackPipeline` / `ElibAttackListener` / `ElibAttackGate` /
  `ElibAttackBlockInterest`；`BlockStateAttackMixin` 把方块左键接进管线；
- 内置元素模块 `elementlib:element`，数据对象仍是 `StatusContainer`。

**变更**

- 实体/方块/角色的元素容器改为从模块层取；`ElementalAttachments.container(...)` 与
  `BlockElementStore` 变为兼容壳；
- 方块元素容器从 `chunk_elements` 迁到 `chunk_modules`；水位仍留在旧附件。

**兼容**

- 旧 `elementlib:status_container` 与 `chunk_elements.containers` 首次访问时迁移，只读不写。

### 二、事件网络（2026-10-06 追加）

元素附着、元素反应、攻击行为、击中目标四类事实在发生的那一刻广播一次，
消费方（角色天赋、圣遗物、武器被动、能量球、统计）一律订阅，不再反复扫描或互相硬编码。

**新增**

- 事件总入口 `ElibEvents`（`post` / `register` / `unregister`）：事件系统就是 **NeoForge 的事件总线**
  （`NeoForge.EVENT_BUS`），不另造并行总线；`post` 在广播点兜住监听者异常；
- 事件：`ElibElementAttachedEvent`（`elementlib:element_attached`）、
  `ElibElementReactionEvent`（`elementlib:element_reacted`）、
  `ElibAttackPerformedEvent`（`elementlib:attack_performed`，含对空）、
  `ElibAttackHitEvent`（`elementlib:attack_hit`，实体与方块都在这里）；
- `ElibIdentifiedEvent`：事件必须报自己的事件 id 与中文名（日志用）；
- `AttachWrite`（`NEW` / `REFRESHED` / `OVERWRITTEN`）：一次附着相对已有附着做了什么；
- `AttachContext.originId`：附着/反应的来源标识（什么环境、哪一招），与决定覆盖规则的
  `AttachmentSource` 分工不同；
- 日志组 `LogGroup.EVENT`：一个事件广播打一行（`[事件] <中文名> <id> | <内容>`）。

**变更**

- `ElibAttackAction` 新增 `damageSubStep` 与 `originId` 两个字段，以及
  `withKindId` / `withOriginId` / `asDamageSubStep`；
- 实体附着分支现在遵守 `elementAmount > 0`（0 = 这次攻击不附着），与方块分支口径一致；
  方块外来附着带来源 `elementlib:environment/block`，方块自带元素带
  `elementlib:environment/block_self_aura`；
- `ReactionContext` 新增 `originId`（旧 9 参构造保留）。

**备注**

- NeoForge 的 `IEventBus` 没有 `hasListeners`，调用点无法提前短路；因此事件对象保持廉价，
  能廉价判断「这次没有落点」的场合由调用点自己先返回；
- 事件是只读通知，不提供取消：要拦行为继续走宿主筛查、`BlockEvent.BreakEvent`、
  `ControlService` 这些既有的裁决通道。
