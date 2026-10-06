# ElementLib 模块层契约（26.2.0.4）

版本：`26.2.0.4`（兼容版；下一次 `26.2.0.5` 删除旧附件读取）
配套：`G:\AI\Codex\Skill\像素原神\元素起源\041/042/043`

## 1. 一句话

把「一个宿主一个容器、容器里装多个带注册类型的实例」这条已在 `StatusContainer` 上验证过的形状抽成通用层；
元素退化为其中第一个模块，韧性等由使用方注册。

## 2. 公开类型

| 类型 | 位置 | 说明 |
| --- | --- | --- |
| `ElibModuleType<T>` | `core/module/` | 注册项类型：id、支持宿主、持久化、同步、工厂、codec/streamCodec |
| `ElibModuleData` | `core/module/` | 注册项实例数据；`StatusContainer` 已实现 |
| `ElibModuleContainer` | `core/module/` | 宿主身上全部模块数据；序列化/同步自实现 |
| `ElibModuleHost` / `ElibModuleHosts` | `core/module/` | 宿主适配器与工厂 |
| `ElibModuleTargetKind` / `ElibModuleTargetKinds` | `core/module/` | 宿主种类（`elementlib:entity/block/item`，外部可注册） |
| `ElibModuleRegistry` / `ElibModuleTypes` | `core/module/` | 类型注册表与内置类型（目前只有 `elementlib:element`） |
| `ElibModuleQuery` | `core/module/` | `around / in / dataIn` |
| `ElibModuleAttachments.MODULES` | `core/attachment/` | 实体附件 `elementlib:modules` |

## 3. 注册一个模块

```java
ElibModuleRegistry.register(ElibModuleType.<MyData>builder(Minegenshin.id("toughness"), MyData.class)
        .supports(ElibModuleTargetKinds.ENTITY, ElibModuleTargetKinds.BLOCK)
        .persistent()
        .sync(ElibModuleSync.SYNC_TO_CLIENT)
        .factory(MyData::new)
        .codec(MyData.CODEC)
        .streamCodec(MyData.STREAM_CODEC)
        .build());
```

宿主种类 id 是 `Identifier`：MineGenshin 用
`ElibModuleTargetKinds.register(Minegenshin.id("character"))` 注册角色宿主，
再用 `ElibModuleType.support(kind)` 把已有模块扩展到新宿主。

## 4. 存储与生命周期

| 宿主 | 存储 | 状态 |
| --- | --- | --- |
| 实体 | 附件 `elementlib:modules` | 已接（含旧 `status_container` 迁移） |
| 方块 | `elementlib:chunk_modules`（持久区 + 瞬态区） | 已接（含旧 `chunk_elements` 迁移） |
| 物品 | DataComponent `elementlib:item_modules` | 已接（阶段 1 只读） |
| 角色 | `PGCharacterData.moduleContainer` | MineGenshin 侧注册 |

`PERSISTENT` 写存档；`TRANSIENT_PER_LOAD` 不写存档，宿主卸载即丢 —— 方块韧性靠它实现
「只在卸载重载后恢复满值」。

## 5. 序列化与同步

- `ElibModuleContainer` 实现 `IPersistedSerializable`，自写 `serialize/deserialize/writeToBuff/readFromBuff`；
- 每条数据按 `type id + 该类型 codec/streamCodec` 落盘与同步；
- 元素模块用 `StatusContainer.CODEC` / `StatusContainer.STREAM_CODEC`，因此元素数据格式与旧版一致。

## 6. 兼容

- 旧附件 `elementlib:status_container` 仍注册，只读；
- 实体第一次访问模块容器时，把旧 `StatusContainer` 包成 `element` 条目写进新容器；
- 方块第一次访问某格时，把旧 `chunk_elements` 的元素容器迁进 `chunk_modules`；
- `26.2.0.5` 删除旧读取分支，见 `TODO-兼容清单.md`。

## 7. 攻击入口

- `ElibAttackPipeline.dispatch(action)`：门禁 → 收集宿主（实体 + 方块）→ 元素附着 → 通知监听器；
- `BlockStateAttackMixin` 注入 `BlockBehaviour.BlockStateBase#attack`，每次有效左键交互 dispatch 一次；
- 使用方可以注入门禁（原神模式）、元素解析器、方块关注点与监听器，也可以关掉实体附着。
