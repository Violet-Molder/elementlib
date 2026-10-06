# 更新日志

> 版本命名自 26.2.0.3 起为「MC 版本 + mod 版本」（旧命名 1.0.3 = 26.2.0.3）。

---

## 26.2.0.4 — 2026-10-06（兼容版本）

本版新增通用模块层与统一攻击入口；元素成为其中一个模块。本版为兼容版本，旧存档仍可读，
计划在 26.2.0.5 删除旧附件读取（见 `TODO-兼容清单.md`）。

### 新增

- 模块层：`ElibModuleType` / `ElibModuleData` / `ElibModuleContainer` / `ElibModuleHost` /
  `ElibModuleHosts` / `ElibModuleQuery` / `ElibModuleRegistry` / `ElibModuleTargetKinds`；
- 存储：实体附件 `elementlib:modules`、区块 `elementlib:chunk_modules`（持久区 + 瞬态区）、
  物品组件 `elementlib:item_modules`（只读）；外部宿主可用 `ElibModuleHosts.registerCarrier` 注册；
- 攻击：`ElibAttackAction` / `ElibAttackPipeline` / `ElibAttackListener` / `ElibAttackGate` /
  `ElibAttackBlockInterest`；`BlockStateAttackMixin` 把方块左键接进管线；
- 内置元素模块 `elementlib:element`，数据对象仍是 `StatusContainer`。

### 变更

- 实体/方块/角色的元素容器改为从模块层取；`ElementalAttachments.container(...)` 与
  `BlockElementStore` 变为兼容壳；
- 方块元素容器从 `chunk_elements` 迁到 `chunk_modules`；水位仍留在旧附件。

### 兼容

- 旧 `elementlib:status_container` 与 `chunk_elements.containers` 首次访问时迁移，只读不写。
