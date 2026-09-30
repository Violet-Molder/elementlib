# ElementLib —— 元素体系独立库

从主模组 `minegenshin` **复制**（不是移动）出来的元素系统，做成一个可以**单独进游戏**的
Gradle 子模块。原工程的元素代码一行没动。

## 这个库包含什么

| 能力 | 位置 |
| --- | --- |
| 元素注册 | `core/element/`（`GenshinElement` / `ModElements` / `ColdElement`）|
| 元素附着 | `core/system/about/`（`ElementalAttachmentHelper` 是唯一入口）|
| 附着状态容器 | `core/attachment/` + `core/status/` |
| 元素反应（注册制） | `core/system/reaction/`，7 条示范反应在 `builtin/` |
| 反应类型注册表 | `api/ElementalReactionType` + `core/system/registry/register/ModReactionTypes` |
| 计时计数器 | `core/system/combat/decay/`（`DecayCounter*`）|
| 衰减序列 | `core/system/combat/decay/DecaySequence` / `DecayGroup` —— **只含元素附着量序列** |
| 环境元素附着 | `core/system/about/block/`（方块元素容器、自附着、状态迁移）|
| 示范实体 | `core/entity/`（雷暴云、星璇）|
| 示范物品 | `content/items/`（元素法杖）|
| 元素附着图标 | `api/AuraIconRules` + `client/ElementAuraIconRenderer` |
| **对外 API** | **`api/`** —— 见下方「接入本体的方式」|

**不包含**：`PGCharacter` 角色系统、伤害管线、削韧/韧性序列、武器被动、角色效果。
反应伤害统一走 `api/ElementalDamageHandler`，默认用配置里的示范值，本体接进来时替换这一个接口即可。

## 配置文件（`run/config/elementlib/`）

| 文件 | 配置项 | 默认 | 含义 |
| --- | --- | --- | --- |
| `elements.toml` | `register-demo-elements` | true | 是否注册示范元素（关掉则整套示范内容都不注册，使用者自己注册） |
| | `show-aura-icon` | true | 是否在目标头上显示元素附着图标（总闸） |
| `reactions.toml` | `register-demo-reactions` | true | 是否注册示范反应 |
| | `reaction-melt` / `reaction-vaporize` … | | 增幅倍率 |
| | `damage-superconduct` / `damage-lunar-charged` … | | 各反应的示范伤害值 |
| `items.toml` | `register-demo-items` | true | 是否注册示范物品 |

> 注册必须发生在配置真正加载之前，所以这三个开关由 `ElementLibEarlyFlags` 在构造期直接读同名文件；
> 改完配置**重启**即可生效。

## 独立进游戏

```bash
gradlew :elementlib:runClient     # 只加载 elementlib 启动客户端
gradlew :elementlib:compileJava   # 只编这个模块
```

## 游戏内测试

```
/elementlib reactions                          # 看已注册反应
/elementlib attach hydro                       # 给自己挂 1U 水
/elementlib attach pyro 2                      # 再挂 2U 火 → 蒸发（倍率出口）
/elementlib attach cyro 1                      # 挂冰 → 冻结
/elementlib info                               # 看附着量 / 衰减速率 / 剩余时间
/elementlib decay normal 10                    # 计时计数器 + 元素量系数序列
/elementlib block cyro 1                       # 脚下方块挂冰 → 水结冰
/elementlib demo status                        # 星扩散 / 星超导 / 月感电 开关状态（默认全关）
/elementlib demo stellar_swirl <on|off>         # 打开后：扩冰 → 星扩散风伤 + 生成星璇（爆炸冰伤）
/elementlib demo stellar_conduce <on|off>       # 打开后：超导顶替为星超导（雷伤 + 冰伤）
/elementlib demo lunar <on|off>                 # 打开后：感电顶替为月感电（生成雷暴云，伤害更高）
/elementlib icon <on|off>                      # 元素附着图标总闸
/elementlib wand                               # 给自己发一把元素法杖
```

示范物品：`element_wand`（右键实体制→挂当前元素；潜行右键→切换元素）。

## 反应与「出口」

已实现（示范）：融化、蒸发、冻结、超导、感电、扩散、月感电；
星体系走同一批反应的分支（星扩散 = 风伤 + 生成星璇实体，爆炸冰伤；星超导 = 雷伤 + 冰伤）。
伤害值只是配置里的示范数字，**计算与结算都在 `api/ElementalDamageHandler` 里**：

```java
ElementLibApi.setDamageHandler(new ElementalDamageHandler() {
    @Override public float computeDamage(ElementalDamageContext ctx) {
        // 换成你的公式：等级系数 × 精通加成 × …
        return ctx.baseDamage() * 2.0f;
    }
    @Override public void dealDamage(ElementalDamageContext ctx, float amount) {
        // 换成你的伤害管线
    }
    @Override public float computeAmplifyMultiplier(ElementalReactionType type, float configured) {
        return configured; // 融化/蒸发的增幅倍率出口
    }
});
```

星体系/月体系在本体里依赖角色（哥伦比娅、星扩散户口），库里用可替换的门：

```java
ElementLibApi.setVariantGate(new ReactionVariantGate() {
    @Override public boolean lunarCharged(Entity attacker, LivingEntity target) { /* 查你的队伍 */ }
    …
});
```

## 接入本体的方式

- **附着/查询**：`ElementLibApi.attach(...)` / `consume(...)` / `container(entity)`。
- **注册自己的元素/反应/类型**：`ElementLibRegistries.ELEMENTS` / `REACTIONS` / `REACTION_TYPES`
  三个 `DeferredRegister` 直接注册即可（反应类型是注册表对象，不是枚举）。
- **伤害**：换 `ElementalDamageHandler`（见上）。
- **图标**：`AuraIconRules.register(...)` 加按元素/按来源的条件，
  `registerEnvironmentExempt(element, EntityType...)` 往环境豁免名单里加实体（默认只有水生生物 + 溺尸）。
  元素基类上还留了 `GenshinElement#shouldShowAuraIcon(ctx)` 的按元素覆盖钩子（预留）。

## 运行约束

- 依赖：NeoForge + Minecraft + **LDLib2**（`StatusContainer` 的多态序列化用它的
  `IPersistedSerializable` / `PersistedParser`）。
- 命名空间：`elementlib`（元素 id 形如 `elementlib:pyro`）。
- 设计/抽取契约见 [DESIGN.md](DESIGN.md) 与 [DESIGN-2.md](DESIGN-2.md)。
