package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 元素反应基类。
 *
 * <p>注册参数：{@code elementA} / {@code elementB} 是参与反应的两个元素，消耗比
 * {@code ratioA:ratioB} 按注册顺序对应它们；{@code elementB} 是消耗更多的一方，
 * 先手为 B、后手为 A 时算克制（倍率更高）。
 *
 * <p>类元素在配对前先归并到主元素（冻→冰、激→雷、燃→火、木→草）。
 * 执行阶段双方按比例同时消耗，哪一边先耗尽就停。
 */
public abstract class ElementalReaction {

    protected final String elementAId;
    protected final String elementBId;
    private transient GenshinElement cachedElementA;
    private transient GenshinElement cachedElementB;
    protected final float ratioA;
    protected final float ratioB;
    protected final int basePriority;

    /** 反应类型以 Supplier 延迟解析，避免注册顺序影响构造。 */
    protected final Supplier<ElementalReactionType> reactionType;
    @Nullable
    private transient ElementalReactionType resolvedReactionType;

    protected ElementalReaction(Supplier<ElementalReactionType> reactionType,
                                String elementAId, String elementBId,
                                float ratioA, float ratioB,
                                int basePriority) {
        this.reactionType = reactionType;
        this.elementAId = elementAId;
        this.elementBId = elementBId;
        this.ratioA = ratioA;
        this.ratioB = ratioB;
        this.basePriority = basePriority;
    }

    @Nullable
    public GenshinElement getElementA() {
        if (cachedElementA == null) cachedElementA = resolveElement(elementAId);
        return cachedElementA;
    }

    @Nullable
    public GenshinElement getElementB() {
        if (cachedElementB == null) cachedElementB = resolveElement(elementBId);
        return cachedElementB;
    }

    @Nullable
    private static GenshinElement resolveElement(String id) {
        String[] parts = id.split(":", 2);
        Identifier identifier = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
        return ModRegistries.ELEMENT_REGISTRY.get(identifier).map(r -> r.value()).orElse(null);
    }

    public float getRatioA() { return ratioA; }
    public float getRatioB() { return ratioB; }
    public int getBasePriority() { return basePriority; }

    /** 解析后的反应类型；注册表里没有这个类型时为 {@code null}。 */
    @Nullable
    public ElementalReactionType getReactionType() {
        if (resolvedReactionType == null) resolvedReactionType = reactionType.get();
        return resolvedReactionType;
    }

    /** 子类取自己的反应类型，等价于 {@link #getReactionType()}。 */
    @Nullable
    protected ElementalReactionType type() {
        return getReactionType();
    }

    public boolean canMatch(GenshinElement attackerElement, GenshinElement defenderElement) {
        GenshinElement attackerMain = attackerElement.getMainElement();
        GenshinElement defenderMain = defenderElement.getMainElement();
        if (attackerMain == null || defenderMain == null) return false;
        GenshinElement a = getElementA();
        GenshinElement b = getElementB();
        return (attackerMain == a && defenderMain == b)
                || (attackerMain == b && defenderMain == a);
    }

    /**
     * 计算双方的实际消耗：{@code min(unitA / ratioA, unitB / ratioB)} 轮，再按比例摊到两边。
     *
     * @return {@code [elementA 的消耗量, elementB 的消耗量]}
     */
    public float[] calculateConsumption(float unitA, float unitB) {
        if (unitA <= 0 || unitB <= 0) return new float[]{0f, 0f};
        float rounds = Math.min(unitA / ratioA, unitB / ratioB);
        float consumedA = rounds * ratioA;
        float consumedB = rounds * ratioB;
        return new float[]{consumedA, consumedB};
    }

    /**
     * 执行反应：消耗元素并结算效果。
     *
     * @return 反应结果（消耗量、是否增幅、增幅倍率等）
     */
    public abstract ReactionResult execute(ReactionContext context);

    /**
     * 先手元素实例能不能参与 {@code slotElement} 槽位的消耗。
     *
     * <p>默认按主元素归并匹配（融化语义）；{@link com.linweiyun.elementlib.core.system.reaction.builtin.FreezeReaction}
     * 覆盖成精确匹配，排除冻。
     */
    public boolean canConsume(ElementalAttachmentInstance instance, GenshinElement slotElement) {
        return instance.getElement().getMainElement() == slotElement.getMainElement();
    }

    /** 求和容器中所有能参与 {@code slotElement} 消耗的实例的元素量。 */
    public float sumConsumable(StatusContainer container, GenshinElement slotElement) {
        float sum = 0f;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (!canConsume(ea, slotElement)) continue;
            sum += ea.getUnit();
        }
        return sum;
    }

    /** 从容器中扣减 {@code amount} 量的、能参与 {@code slotElement} 消耗的实例。 */
    public void consumeElementUnit(StatusContainer container, GenshinElement slotElement, float amount) {
        float remaining = amount;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            if (!canConsume(ea, slotElement)) continue;
            float consumed = ea.consume(remaining);
            remaining -= consumed;
            if (remaining <= 0f) break;
        }
    }

    /** 聚变反应的伤害冷却（毫秒）；0 表示无限制。 */
    public int getDamageCooldownMs() { return 0; }

    /** 聚变反应的公共冷却（毫秒）；0 表示无限制。 */
    public int getReactionCooldownMs() { return 0; }

    /** 该反应在当前目标状态下是否被禁止（例：冻结状态下禁止蒸发）。 */
    public boolean isBlocked(ReactionContext context) { return false; }

    /**
     * 反应成立之后的宿主侧效果，默认什么都不做。
     *
     * <p>只处理反应之外的表现（例：火把冰烧成水），不要在这里写附着。
     */
    public void applyHostEffect(ReactionContext context) {
    }

    /** 本次反应要不要生成飘字（默认 true）。方块上的形态变化不显示文字。 */
    public boolean showsIndicator(ReactionContext context) {
        return true;
    }
}
