# TODO · 兼容清单（26.2.0.4）

规则：本版本读旧写新；每条在**计划删除版本**发版时删掉，并在 CHANGELOG 注明。

| # | 兼容项 | 谁在用 | 26.2.0.4 行为 | 计划删除 | 删除前确认 |
| --- | --- | --- | --- | --- | --- |
| 1 | 旧实体附件 `elementlib:status_container` | elementlib 自身、MineGenshin | 读旧键，只写新键 `elementlib:modules`；首次访问时迁移 | `26.2.0.5` | 至少跑过一次旧存档升级；新键稳定 |
| 2 | 旧 API `ElementalAttachments.container(...)` 的旧语义 | 外部使用者 | 仍可用，内部走模块入口 | 下个大版本 | 替代写法写进 README |
| 3 | `ElementalAttackSweep` / `BlockElementHelper.applyElement(...)` 方块入口 | MineGenshin | 迁移期保留，内部转调新入口 | 下个大版本 | 方块模块存储接完 |

## 已接入（26.2.0.4）

- 方块模块存储 `ChunkModules` / `BlockModuleHost`：已接，旧 `chunk_elements` 按格迁移；
- 物品宿主 `ItemModuleHost`：已接，阶段 1 只读（写入契约待定）；
- 攻击链路 `ElibAttackAction` / `ElibAttackPipeline` / `BlockStateAttackMixin`：已接。
