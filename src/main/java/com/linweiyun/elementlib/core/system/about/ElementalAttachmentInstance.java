package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class ElementalAttachmentInstance extends StatusInstance {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);
    private static final String TYPE_ID = "elemental_attachment";

    @Persisted(key = "element_id")
    private String elementId;

    private transient GenshinElement element;

    @Getter
    @Persisted(key = "source")
    private AttachmentSource source;

    @Getter
    @Persisted(key = "profile")
    private AttachmentProfile profile;

    @Getter
    @Persisted(key = "unit")
    private float unit;

    @Getter
    @Persisted(key = "current_decay_per_second")
    private float currentDecayPerSecond;

    @Getter
    @Persisted(key = "permanent")
    private boolean permanent;

    @Persisted(key = "replenish_tick")
    private int replenishTick;

    @Persisted(key = "replenish_amount")
    private float replenishAmount;

    @Persisted(key = "replenish_timer")
    private int replenishTimer;

    @Getter
    @Persisted(key = "source_character_key")
    private String sourceCharacterKey;

    @Setter
    @Getter
    @Persisted(key = "attach_tick")
    private long attachTick;

    @Getter
    @Persisted(key = "frozen_cyro_sources")
    private java.util.List<String> frozenCyroSourceKeys = new java.util.ArrayList<>();

    @Getter
    @Persisted(key = "frozen_hydro_sources")
    private java.util.List<String> frozenHydroSourceKeys = new java.util.ArrayList<>();

    @Setter
    private transient StatusContainer container;

    @Getter
    @Setter
    private transient ElementalHost host;

    public ElementalAttachmentInstance(GenshinElement element, AttachmentSource source,
                                       AttachmentProfile profile, float initialUnit,
                                       String sourceCharacterKey, long attachTick) {
        this.typeId = TYPE_ID;
        this.element = element;
        this.elementId = resolveElementId(element);
        this.source = source;
        this.profile = profile;
        this.unit = initialUnit;
        this.currentDecayPerSecond = profile.getDecayPerSecond();
        this.permanent = profile.isPermanent();
        this.replenishTick = 200;
        this.replenishAmount = 1.0f;
        this.replenishTimer = replenishTick;
        this.sourceCharacterKey = sourceCharacterKey;
        this.attachTick = attachTick;
    }

    public ElementalAttachmentInstance(GenshinElement element, AttachmentSource source,
                                       AttachmentProfile profile, float initialUnit) {
        this(element, source, profile, initialUnit, null, 0L);
    }

    public ElementalAttachmentInstance() {
        this.typeId = TYPE_ID;
        this.elementId = "elementlib:fysikos";
        this.source = AttachmentSource.SPECIAL;
        this.profile = new AttachmentProfile(0f, 0f, 0f, 0f);
    }

    @Override
    public void tick() {
        if (permanent) {
            replenishTimer--;
            if (replenishTimer <= 0) {
                unit = Math.min(unit + replenishAmount, profile.getBaseQuantity());
                replenishTimer = replenishTick;
            }
            return;
        }
        float effectiveDecayPerSecond;
        GenshinElement e = getElement();
        if (ModElements.is(e, ModElements.FROZEN)
                && container != null
                && container.getFrozenDecayState() != null) {
            effectiveDecayPerSecond = container.getFrozenDecayState().getCurrentDecayRate();
        } else {
            effectiveDecayPerSecond = currentDecayPerSecond;
        }
        float decayPerTick = effectiveDecayPerSecond / 20f;

        unit = Math.max(0f, unit - decayPerTick);
    }

    @Override
    public boolean isFinished() {
        return unit <= 0f;
    }

    @Override
    public StatusInstance copy() {
        ElementalAttachmentInstance c = new ElementalAttachmentInstance();
        c.element = this.element;
        c.elementId = this.elementId;
        c.source = this.source;
        c.profile = this.profile;
        c.unit = this.unit;
        c.currentDecayPerSecond = this.currentDecayPerSecond;
        c.permanent = this.permanent;
        c.replenishTick = this.replenishTick;
        c.replenishAmount = this.replenishAmount;
        c.replenishTimer = this.replenishTimer;
        c.sourceCharacterKey = this.sourceCharacterKey;
        c.attachTick = this.attachTick;
        c.frozenCyroSourceKeys = new java.util.ArrayList<>(this.frozenCyroSourceKeys);
        c.frozenHydroSourceKeys = new java.util.ArrayList<>(this.frozenHydroSourceKeys);
        c.container = null;
        return c;
    }

    @Override
    public void onRemove() {
        if (element != null && host != null) {
            host.onElementDetached(element);
        }
    }
    public void refreshQuantity(float newQuantity) {
        this.unit = newQuantity;
    }

    /** 覆盖时同步更新来源标识与附着时间戳 */
    public void refreshSource(String newSourceCharacterKey, long newAttachTick) {
        this.sourceCharacterKey = newSourceCharacterKey;
        this.attachTick = newAttachTick;
    }

    /** 覆盖规则：火/激/燃 覆盖时直接替换衰减速率 */
    public void overrideDecayRate(float newRate) {
        this.currentDecayPerSecond = newRate;
    }

    /** 消耗（元素反应调用），返回实际消耗量 */
    public float consume(float amount) {
        float actual = Math.min(amount, unit);
        unit -= actual;
        return actual;
    }

    // ========== Getter ==========

    public GenshinElement getElement() {
        if (element == null && elementId != null && !elementId.isEmpty()) {
            String[] parts = elementId.split(":", 2);
            Identifier id = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            element = ModRegistries.ELEMENT_REGISTRY.get(id).map(Holder.Reference::value).orElse(null);
        }
        return element;
    }

    public LivingEntity getOwner() { return host == null ? null : host.entity(); }

    private static String resolveElementId(GenshinElement element) {
        Identifier key = ModRegistries.ELEMENT_REGISTRY.getKey(element);
        return key != null ? key.toString() : "elementlib:fysikos";
    }

    public boolean hasSourceCharacter() {
        return sourceCharacterKey != null && !sourceCharacterKey.isEmpty();
    }

    public long getDecayEndTick() {
        float decay = currentDecayPerSecond;
        if (decay <= 0f) return Long.MAX_VALUE;
        float remainingSeconds = unit / decay;
        return attachTick + (long) (remainingSeconds * 20f);
    }

    public void addFrozenCyroSource(String key) {
        if (key != null && !key.isEmpty() && !frozenCyroSourceKeys.contains(key)) {
            frozenCyroSourceKeys.add(key);
        }
    }

    public void addFrozenHydroSource(String key) {
        if (key != null && !key.isEmpty() && !frozenHydroSourceKeys.contains(key)) {
            frozenHydroSourceKeys.add(key);
        }
    }
}
