# ElementLib 第二轮设计契约（配置 / 注册制反应 / 示范内容 / API 包）

本文件是第二轮改造的**唯一真相**。所有实现必须严格遵守。

第一轮的契约见 `DESIGN.md`；第一轮已落地的代码保持可用，本轮在其上扩展。

## 0. 本轮目标

1. **三个配置文件**：是否注册①示范元素、②示范元素反应、③示范物品。
2. **反应类型注册制**：删除 `ElementalReactionType` 枚举，改成**注册表对象**（可被使用者新增、可查询）。
3. **补完示范内容**：把项目本体已实现的反应效果全部搬进来（含星扩散、星超导、月感电），伤害用**示范固定值**，
   但必须留出**可替换的伤害计算出口**；同时把示范元素/物品/实体写完整，作为「出口怎么用」的样例。
4. **元素附着图标**：总闸配置 + 元素基类上的预留钩子 + 按元素/按来源注册的条件 + 环境附着的默认豁免名单
   （水对水生生物不显示，名单可加，如溺尸）+ 真正在目标头上渲染图标。
5. **`api` 包**：把「使用者要接的东西」整理进 `com.linweiyun.elementlib.api`，方便本体接入。

## 1. 硬性规则

- 只能改 `elementlib/**`；`src/**` 只读，一个字都不许改。
- elementlib 内**禁止** `import com.linweiyun.genshin.*`；除 LDLib2 的 `syncdata` / `PersistedParser` / `Platform` 外不引其他第三方。
- 不跑 Gradle、不编译（父代理最后统一验收）。
- 第一轮已确定的公开签名不要随意改；本轮明确要求改的除外。
- 所有新增类都在 `com.linweiyun.elementlib.*` 下。

## 2. 关键决策：演示元素可以「不注册」

`register-demo-elements = false` 时，**元素/反应/物品/实体/方块环境规则整套都不注册**（使用者自己注册元素）。
因此**核心代码必须对「元素不存在」是安全的**：

- `ModElements` 新增（本轮新增，全库统一用它取元素）：

```java
/** 演示元素未启用时返回 null —— 不要在这些路径上直接 holder.get()。 */
@Nullable public static GenshinElement of(DeferredHolder<GenshinElement, ? extends GenshinElement> holder);
/** null 安全比较：holder 未注册时恒 false（绝不能退化成 element == null 成立）。 */
public static boolean is(@Nullable GenshinElement element, DeferredHolder<GenshinElement, ? extends GenshinElement> holder);
```

- 库内所有 `ModElements.X.get()` 一律改成 `ModElements.of(ModElements.X)` 或 `ModElements.is(x, ModElements.X)`；
  参数位置拿到 null 必须有守卫（直接 return / 跳过）。
- `ModElements.register(bus)` 在演示元素关闭时**不注册任何东西**；`setupSubElements()` 同样跳过。

## 3. 三个配置文件

`ElementLibConfig`（`config/ElementLibConfig.java`）持有三个 Spec：

| 文件 | 配置项 | 默认 |
| --- | --- | --- |
| `elementlib/elements.toml` | `register-demo-elements`（bool） | true |
| | `show-aura-icon`（bool） | true |
| `elementlib/reactions.toml` | `register-demo-reactions`（bool） | true |
| | 各反应示范伤害值（见 §5） | 见表 |
| `elementlib/items.toml` | `register-demo-items`（bool） | true |

三者在 `ElementLib` 构造里用 `modContainer.registerConfig(ModConfig.Type.COMMON, SPEC, "elementlib/xxx.toml")` 注册。

**注册决策必须发生在配置真正加载之前**，所以另建 `config/ElementLibEarlyFlags`：
在类初始化时直接读 `FMLPaths.CONFIGDIR.get().resolve("elementlib/xxx.toml")`，
按行匹配 `key = true|false`（容忍空格、注释、缺文件），缺文件/读失败一律取默认 `true`。
`ModConfigEvent.Loading` 里再用 Spec 的真实值覆盖一次（保持运行期一致）。

导出给全库用的静态入口（`ElementLibConfig`）：

```java
public static boolean demoElementsEnabled();
public static boolean demoReactionsEnabled();
public static boolean demoItemsEnabled();
public static boolean showAuraIcon();       // 运行期读 Spec
public static float baseDamage(String key); // §5 的示范伤害值
```

## 4. 反应类型注册制（删除枚举）

- **删除** `core/system/reaction/ElementalReactionType.java`。
- **新增** `api/ElementalReactionType.java`：

```java
public final class ElementalReactionType {
    public ElementalReactionType(String translationKey, ReactionCategory category);
    public Identifier getId();            // 注册表 key（未注册时 null）
    public String getPath();              // key 的 path（未注册时 ""）
    public String getTranslationKey();    // 如 "reaction.minegenshin.melt"
    public ReactionCategory getCategory();
    public boolean is(ReactionCategory category);
    @Override public String toString();   // = getPath()
}
```

- **新增** `api/ReactionCategory.java`：`AMPLIFYING, TRANSFORMATIVE, SPECIAL, STELLAR, LUNAR, OTHER`。
- `ModRegistries` 新增 `REACTION_TYPE_REGISTRY_KEY` / `REACTION_TYPE_REGISTRY` / `REACTION_TYPES`，
  并在 `NewRegistryEvent` 里注册（`sync(true)`, `maxId(64)`）。
- **新增** `core/system/registry/register/ModReactionTypes.java`，注册**全部**原枚举里的 23 个类型
  （id 用 snake_case：`melt/vaporize/shattered/superconduct/swirl/electro_charged/overload/burning/bloom/hyperbloom/
  burgeon/quicken/aggravate/spread/stellar_swirl_wind/stellar_swirl_ice/stellar_conduce_electro/stellar_conduce_ice/
  lunar_charged/lunar_bloom/lunar_crystallize/frozen/crystallize`），
  翻译键沿用 `reaction.minegenshin.*`（lang 已存在），分类：
  AMPLIFYING = melt/vaporize；SPECIAL = frozen/shattered/crystallize；STELLAR = 4 个 stellar_*；
  LUNAR = 3 个 lunar_*；其余 TRANSFORMATIVE。
  整个注册同样受 `register-demo-reactions` 与 `register-demo-elements` 门控（元素关了反应也没法注册）。
- `ElementalReaction` 的类型字段改为 **Supplier**（避免注册顺序问题）：

```java
protected final Supplier<ElementalReactionType> reactionType;
protected ElementalReaction(Supplier<ElementalReactionType> reactionType,
                            String elementAId, String elementBId,
                            float ratioA, float ratioB, int basePriority);
public ElementalReactionType getReactionType();     // 解析后返回
protected ElementalReactionType type();             // 给子类用
```
  子类里 `reactionType` 的用法改成 `type()`；`ReactionResult.builder(type())`。
- `ElementalReactionManager.shouldShowIndicator` 改成按**分类**判断：
  `LUNAR` 与 `STELLAR` 分类不出字（它们的表现由自己的伤害链出）。
- `ReactionFeedback` 用 `type.getTranslationKey()`（原来的 `getDisplayName()` 不再存在）。
- **查询入口**（写进 api，见 §7）：`ElementLibApi.reactionType(Identifier)` / `reactionTypes()`。

## 5. 示范反应：补完 + 伤害出口

### 5.1 伤害出口（api）

```java
// api/ElementalDamageReason.java
public enum ElementalDamageReason { REACTION, AREA_TICK, AREA_EXPLODE }

// api/ElementalDamageContext.java
public record ElementalDamageContext(ElementalReactionType reactionType,
                                     @Nullable GenshinElement element,
                                     @Nullable Entity attacker,
                                     @Nullable LivingEntity target,
                                     @Nullable Vec3 position,
                                     ElementalDamageReason reason,
                                     float baseDamage) {
    @Nullable public ServerLevel level();
}

// api/ElementalDamageHandler.java
public interface ElementalDamageHandler {
    /** 伤害计算出口：默认直接采用配置给的示范值。返回 <=0 表示这一下不结算。 */
    default float computeDamage(ElementalDamageContext ctx) { return ctx.baseDamage(); }
    /** 实际把伤害打出去：默认走 level.damageSources().magic() 的 hurtServer。 */
    default void dealDamage(ElementalDamageContext ctx, float amount) { /* 见下 */ }
    /** 增幅反应倍率出口（融化/蒸发）：默认用 ReactionResult 里算好的配置倍率。 */
    default float computeAmplifyMultiplier(ElementalReactionType type, float configured) { return configured; }
}
```

- 默认处理器的静态单例放 `core/system/reaction/damage/DemoDamageHandler.java`（`ReactionDamage.DEFAULT`）。
- 发放入口放 `core/system/reaction/damage/ReactionDamage.java`：

```java
public final class ReactionDamage {
    public static void dealDirect(ElementalReactionType type, @Nullable GenshinElement element,
                                  @Nullable Entity attacker, @Nullable LivingEntity target, float baseDamage);
    public static void dealAt(ElementalReactionType type, @Nullable GenshinElement element,
                              @Nullable Entity attacker, Vec3 pos, double radius,
                              float baseDamage, ElementalDamageReason reason);
    public static float amplify(ElementalReactionType type, float configured);
}
```
  `dealAt` 会扫描半径内的 LivingEntity（排除攻击者自己），逐个走 handler。

### 5.2 示范伤害值（`reactions.toml`，`ElementLibConfig.baseDamage(key)`）

| key | 值 | 说明 |
| --- | --- | --- |
| `superconduct` | 10 | 普通超导 |
| `electro_charged` | 12 | 普通感电 |
| `swirl` | 14 | 普通扩散 |
| `stellar_swirl_wind` | 20 | 星扩散·风 |
| `stellar_swirl_ice` | 30 | 星扩散·冰（爆炸那一下） |
| `stellar_conduce_electro` | 24 | 星超导·雷 |
| `stellar_conduce_ice` | 28 | 星超导·冰 |
| `lunar_charged` | 40 | 月感电（**必须明显高于 electro_charged**） |

### 5.3 变体开关（星体系/月体系在本体里依赖角色，这里用可替换的出口）

```java
// api/ReactionVariantGate.java
public interface ReactionVariantGate {
    boolean stellarSwirl(@Nullable Entity attacker, @Nullable LivingEntity target);
    boolean stellarConduce(@Nullable Entity attacker, @Nullable LivingEntity target);
    boolean lunarCharged(@Nullable Entity attacker, @Nullable LivingEntity target);
}
// api/DemoContentToggles.java —— 示范实现的可开关状态（命令/物品切换）
public final class DemoContentToggles {
    public static void setStellarSwirl(boolean v); public static boolean stellarSwirl();
    public static void setStellarConduce(boolean v); public static boolean stellarConduce();
    public static void setLunarCharged(boolean v);  public static boolean lunarCharged();
    public static String describe();     // 给命令回显
}
```
默认 gate（`DemoVariantGate`）读 `DemoContentToggles`；本体接进来时用 `ElementLibApi.setVariantGate(...)` 换成真实角色判定。

### 5.4 要实现的反应（`core/system/reaction/builtin/`）

| 类 | 行为 |
| --- | --- |
| `VaporizeReaction` | 保持原样（倍率走 `ReactionDamage.amplify`）。 |
| `MeltReaction` | 保持原样（含方块冰化水效果）。 |
| `FreezeReaction` | 保持原样。 |
| `SuperConductReaction` | 雷+冰。10 tick 冷却不变。若 `variantGate.stellarConduce(...)`：先 `STELLAR_CONDUCE_ELECTRO` 伤害，再 `STELLAR_CONDUCE_ICE` 伤害；否则 `SUPERCONDUCT` 伤害。 |
| `ElectroChargedReaction` | 保持原样（周期伤害走 `ReactionDamage`）。 |
| `SwirlReaction` | 普通扩散照旧。若 `variantGate.stellarSwirl(...)` 且扩散元素是冰：先 `STELLAR_SWIRL_WIND` 伤害，再**生成 `StellarVortexEntity`**（它到时爆炸造成 `STELLAR_SWIRL_ICE` 伤害），不再走普通传播。 |
| `LunarChargedReaction`（新） | 水+雷，`basePriority = -1`（用默认优先级表，排在感电之前）。`variantGate.lunarCharged(...)` 为假时 `isBlocked` 返回 true。生成 `ThunderCloudEntity`，立刻结算一次 `LUNAR_CHARGED` 伤害。 |
| `StellarGlimmerBranch` / 旧枚举相关类 | **不搬**。 |

`ModElementalReactions` 注册：`vaporize / melt / freeze / swirl / electro_charged / superconduct / lunar_charged`
（7 条），全部受 `register-demo-reactions` 门控；元素 id 用 `elementlib:` 命名空间。

## 6. 示范实体（`core/entity/`）

不搬本体的 `AreaEntity` 体系，写一套**精简但完整**的示范：

```java
public abstract class ElementalAreaEntity extends Entity {
    protected ElementalAreaEntity(EntityType<?> type, Level level);
    public void setOwner(@Nullable Entity owner);
    public void setRadius(double radius);
    public void setDurationTicks(int ticks);
    public void setTickInterval(int ticks);
    public void setTickDamage(float damage);
    public void setExplodeDamage(float damage);
    protected abstract ElementalReactionType tickReactionType();
    protected abstract ElementalReactionType explodeReactionType();
    protected abstract GenshinElement element();
    @Override public void tick();   // 到期 tick 间隔 → ReactionDamage.dealAt(AREA_TICK)；寿命到 → 爆炸 dealAt(AREA_EXPLODE) + discard
}
public class ThunderCloudEntity extends ElementalAreaEntity { /* 月感电：周期伤害 + 到期爆炸 */ }
public class StellarVortexEntity extends ElementalAreaEntity { /* 星扩散：周期小伤害 + 到期冰爆炸 */ }
```

- 注册 `core/entity/ModEntities.java`（`DeferredRegister<EntityType<?>>`，`EntityType.Builder.of(...).sized(1,1).build(id)`）。
  用 `EntityType.Builder#build(ResourceKey)` 的写法，参考本体 `ModEntities` 的现有写法（**读本体源码照抄写法**）。
- 客户端渲染：`client/ModEntityRenderers.java` 在 `EntityRenderersEvent.RegisterRenderers` 里给两个实体注册一个
  **空渲染器**（`NoopRenderer extends EntityRenderer`，submit 什么都不画）——可见性靠服务端粒子（`level.sendParticles`），
  这样不需要额外贴图/模型。`@EventBusSubscriber(value = Dist.CLIENT, modid = ElementLib.MOD_ID)`。
- 两个实体类在 `@EventBusSubscriber(Dist.CLIENT)` 之外不要引客户端类。

## 7. `api` 包（本轮要交付的对外面）

`com.linweiyun.elementlib.api`：

| 类 | 作用 |
| --- | --- |
| `ElementLibApi` | **门面**：附着/消耗/查容器/查元素/查反应类型/查反应列表/换伤害处理器/换变体门/图标总闸 |
| `ElementalReactionType` | 反应类型（注册表对象） |
| `ReactionCategory` | 反应分类 |
| `ElementalDamageHandler` / `ElementalDamageContext` / `ElementalDamageReason` | 伤害出口 |
| `ReactionVariantGate` | 星/月变体门（本体用真实角色逻辑替换） |
| `DemoContentToggles` | 示范变体的开关（命令/物品用） |
| `AuraIconCondition` / `AuraIconContext` / `AuraIconRules` | 图标显示条件 |
| `ElementLibRegistries` | 注册表句柄：`ELEMENTS` / `REACTIONS` / `REACTION_TYPES` 的 `DeferredRegister` 与 `ResourceKey` |

`ElementLibApi` 必须包含（签名逐字）：

```java
public final class ElementLibApi {
    // 附着
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile);
    public static AttachResult attach(ElementalHost host, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile, AttachContext ctx);
    public static AttachResult attach(LivingEntity target, GenshinElement element,
                                      AttachmentSource source, AttachmentProfile profile);
    public static float consume(StatusContainer container, GenshinElement element, float amount);
    // 查询
    public static StatusContainer container(LivingEntity entity);
    @Nullable public static GenshinElement element(Identifier id);
    @Nullable public static ElementalReactionType reactionType(Identifier id);
    public static List<ElementalReaction> reactions();
    // 扩展点
    public static void setDamageHandler(ElementalDamageHandler handler);
    public static ElementalDamageHandler damageHandler();
    public static void setVariantGate(ReactionVariantGate gate);
    public static ReactionVariantGate variantGate();
    public static void setAuraIconVisible(boolean visible);
    public static boolean auraIconVisible();
}
```

## 8. 元素附着图标

### 8.1 条件链

`api/AuraIconContext.java`：

```java
public record AuraIconContext(ElementalHost host, @Nullable LivingEntity entity,
                              GenshinElement element, AttachmentSource source,
                              @Nullable AttachmentProfile profile) {}
```

`api/AuraIconCondition.java`：`@FunctionalInterface boolean shouldShow(AuraIconContext ctx);`

`core/element/GenshinElement.java` 增加**预留钩子**（默认 true，目前没有实际用途）：

```java
/** 预留：这个元素要不要在某个宿主上显示附着图标（默认显示）。 */
public boolean shouldShowAuraIcon(AuraIconContext context) { return true; }
```

`api/AuraIconRules.java`：

```java
public final class AuraIconRules {
    /** 按元素附加条件（与其它条件取「与」）。 */
    public static void register(AuraIconCondition condition);
    /** 按来源附加条件。 */
    public static void registerForSource(AttachmentSource source, AuraIconCondition condition);
    /** 环境附着豁免名单：这些实体类型上，该元素的**环境**附着不显示图标（可继续添加，如溺尸）。 */
    public static void registerEnvironmentExempt(GenshinElement element, EntityType<?>... types);
    /** 完整判定：总闸 → 元素钩子 → 元素条件 → 来源条件 → 环境豁免。 */
    public static boolean shouldShow(AuraIconContext ctx);
}
```

默认注册的规则（`AuraIconRules` 静态块，仅当演示元素开启）：

- 环境附着的 `HYDRO`：实体 `getType().getCategory()` 是 `WATER_CREATURE`/`WATER_AMBIENT` → 不显示；
  豁免名单初始为 `EntityType.DROWNED`（**故意只放一个，注释写明「名单可继续加，复制这一行即可」**）。
- 其它元素：**空**。

### 8.2 客户端渲染

- 新增 `client/ElementAuraIconRenderer.java`：`@EventBusSubscriber(value = Dist.CLIENT, modid = ElementLib.MOD_ID)`，
  监听 **`net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent`**，
  照抄本体 `src/main/java/com/linweiyun/genshin/client/render/gui/hud/MobHealthBarHud.java` 里
  `renderElementalIcons`（约 420–505 行）与它上游的「世界坐标 → 相机对齐 billboard」写法（约 160–250 行），
  但**只画元素图标**（不要血条/等级/盾条/韧性）。
- 取图标 `RenderType`：`net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(
  Identifier.fromNamespaceAndPath("elementlib", "icon/elemental/" + element.getId() + ".png"))`，
  按元素缓存（`IdentityHashMap`）。贴图不存在时（frozen/cold/fysikos/aggravate/burning/wood）跳过不画。
- 贴图：把本体 `src/main/resources/assets/minegenshin/icon/elemental/` 下**已存在的 7 张**
  （anemo/cyro/dendro/electro/geo/hydro/pyro）复制到
  `elementlib/src/main/resources/assets/elementlib/icon/elemental/`（二进制复制，用 `Copy-Item`）。
- 只对「客户端也拿得到容器」的实体画：容器附件是 `sync` 的，客户端 `entity.getData(ElementalAttachments.CONTAINER)` 可读。
- 用 `AuraIconRules.shouldShow(...)` 过滤；总闸关掉时整个渲染直接返回。
- 只画**主元素**（类元素归并到主元素，和本体 HUD 一致）。

## 9. 示范物品（`content/items/`）

- `ModItems`（`DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementLib.MOD_ID)`）：
  受 `register-demo-items` 门控。
- `ElementWandItem`（id `element_wand`）：
  - 右键**实体** → 给该实体挂「当前选中元素」（`AttachmentSource.NORMAL_ATTACK` + `AttachmentProfile.WEAK`）。
  - 潜行右键（空气/方块）→ 切换到下一个元素，并把元素名回显到 action bar。
  - `appendHoverText` 显示当前元素。
  - 当前元素存在 `DataComponent`（自己注册一个 `DataComponentType<Identifier>`）或静态 map；**推荐 DataComponent**，并在物品里注册。
- 物品模型资源：`elementlib/src/main/resources/assets/elementlib/items/element_wand.json`
  （`{"model":{"type":"minecraft:model","model":"minecraft:item/blaze_rod"}}`，借原版模型，不需要贴图）。
- lang 增加 `item.elementlib.element_wand`。
- 命令 `/elementlib` 增加（`command/ElementLibCommand.java`）：
  - `/elementlib demo stellar_swirl|stellar_conduce|lunar <on|off>`
  - `/elementlib demo status`
  - `/elementlib icon <on|off>`
  以及保留原有子命令。

## 10. 交付与验收

- 所有新文件写在各自归属的包里，不要互相覆盖（见父代理派单）。
- 最后父代理会跑一次 `:elementlib:compileJava` + `:elementlib:runServer` 验收；
  实现时请自己确认没有 `import com.linweiyun.genshin.*`、没有对已删除枚举的引用。
