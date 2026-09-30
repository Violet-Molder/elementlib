package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * 元素反应基类。
 */
public abstract class ElementalReaction {

    protected final String elementAId;
    protected final String elementBId;
    private transient GenshinElement cachedElementA;
    private transient GenshinElement cachedElementB;
    @Getter
    protected final float ratioA;
    @Getter
    protected final float ratioB;
    @Getter
    protected final int basePriority;
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
    @Nullable
    public ElementalReactionType getReactionType() {
        if (resolvedReactionType == null) resolvedReactionType = reactionType.get();
        return resolvedReactionType;
    }
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

    public float[] calculateConsumption(float unitA, float unitB) {
        if (unitA <= 0 || unitB <= 0) return new float[]{0f, 0f};
        float rounds = Math.min(unitA / ratioA, unitB / ratioB);
        float consumedA = rounds * ratioA;
        float consumedB = rounds * ratioB;
        return new float[]{consumedA, consumedB};
    }

    public abstract ReactionResult execute(ReactionContext context);
    public boolean canConsume(ElementalAttachmentInstance instance, GenshinElement slotElement) {
        return instance.getElement().getMainElement() == slotElement.getMainElement();
    }

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

    public int getDamageCooldownMs() { return 0; }
    public int getReactionCooldownMs() { return 0; }

    public boolean isBlocked(ReactionContext context) { return false; }

    public void applyHostEffect(ReactionContext context) {
    }
    public boolean showsIndicator(ReactionContext context) {
        return true;
    }
}
