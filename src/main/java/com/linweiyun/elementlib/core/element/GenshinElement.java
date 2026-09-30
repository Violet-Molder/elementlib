package com.linweiyun.elementlib.core.element;

import com.linweiyun.elementlib.api.AuraIconContext;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 元素基类 —— 所有元素的统一抽象，通过 Minecraft Registry 注册
 */
public class GenshinElement {

    /** 反应反馈的默认颜色（做了反馈外观但没显式指定的元素使用）。 */
    public static final int DEFAULT_FEEDBACK_COLOR = 0xFFFFFF;

    private GenshinElement mainElement;

    private final boolean canOverrideDecay;
    @Getter
    private final boolean instant;
    @Getter
    private final boolean effectCarrier;
    @Getter
    private final String translationKey;

    /** 反应反馈粒子；为 {@code null} 时反馈出口回退到默认粒子。 */
    @Nullable
    private ParticleOptions feedbackParticle;

    /** 反应反馈颜色（0xRRGGBB）。
     * -- GETTER --
     * 反应反馈颜色（0xRRGGBB）；未指定时为
     * 。
     */
    @Getter
    private int feedbackColor;

    protected GenshinElement(boolean canOverrideDecay, boolean instant, String translationKey) {
        this(canOverrideDecay, instant, false, translationKey);
    }

    protected GenshinElement(boolean canOverrideDecay, boolean instant,
                             boolean effectCarrier, String translationKey) {
        this.canOverrideDecay = canOverrideDecay;
        this.instant = instant;
        this.effectCarrier = effectCarrier;
        this.translationKey = translationKey;
        this.mainElement = null;
        this.feedbackColor = DEFAULT_FEEDBACK_COLOR;
        this.feedbackParticle = null;
    }

    void setMainElement(GenshinElement mainElement) {
        this.mainElement = mainElement;
    }

    public String getId() {
        ResourceLocation key = ModRegistries.ELEMENT_REGISTRY.getKey(this);
        return key != null ? key.getPath() : "";
    }

    public boolean canOverrideDecay() {
        return canOverrideDecay;
    }

    public GenshinElement getMainElement() {
        return mainElement != null ? mainElement : this;
    }

    public boolean isMainElement() {
        return mainElement == null;
    }

    public boolean isSubElement() {
        return mainElement != null;
    }

    public boolean allowsDirectAttachment() {
        return isSubElement();
    }

    public void onAttach(ElementalHost host) {
    }

    /**
     * 元素从宿主身上移除时调用（默认空实现）
     */
    public void onDetach(ElementalHost host) {
    }

    public boolean shouldShowAuraIcon(AuraIconContext context) {
        return true;
    }

    public void setFeedback(int color, @Nullable ParticleOptions particle) {
        this.feedbackColor = color;
        this.feedbackParticle = particle;
    }

    /** 反应反馈粒子；未指定时为 {@code null}（反馈出口回退到默认粒子）。 */
    @Nullable
    public ParticleOptions getFeedbackParticle() {
        return feedbackParticle;
    }

    public static boolean isNonPlayerLiving(LivingEntity entity) {
        return entity != null && !(entity instanceof Player);
    }
}