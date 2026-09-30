package com.linweiyun.elementlib.core.element;

import com.linweiyun.elementlib.config.ElementLibConfig;
import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import com.linweiyun.elementlib.util.log.LogGroup;
import com.linweiyun.elementlib.util.log.ModLog;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * 元素注册中心 —— 使用 DeferredRegister 将所有示范元素注册到 Minecraft Registry。
 */
public final class ModElements {

    private static final Logger LOGGER = ModLog.getLogger(LogGroup.ELEMENT);

    public static final DeferredRegister<GenshinElement> ELEMENTS = ModRegistries.ELEMENTS;

    // ======== 主元素 ========
    public static final DeferredHolder<GenshinElement, GenshinElement> FYSIKOS = ELEMENTS.register(
            "fysikos", () -> feedback(new GenshinElement(false, false, "elemental.gim.fysikos"),
                    0xFFFFFF, ParticleTypes.CRIT));

    public static final DeferredHolder<GenshinElement, GenshinElement> PYRO = ELEMENTS.register(
            "pyro", () -> feedback(new GenshinElement(true, false, "elemental.gim.pyro"),
                    0xFF7043, ParticleTypes.FLAME));

    public static final DeferredHolder<GenshinElement, GenshinElement> HYDRO = ELEMENTS.register(
            "hydro", () -> feedback(new GenshinElement(false, false, "elemental.gim.hydro"),
                    0x4FC3F7, ParticleTypes.SPLASH));

    public static final DeferredHolder<GenshinElement, GenshinElement> ANEMO = ELEMENTS.register(
            "anemo", () -> feedback(new GenshinElement(false, true, "elemental.gim.anemo"),
                    0x74C2A8, ParticleTypes.CLOUD));

    public static final DeferredHolder<GenshinElement, GenshinElement> ELECTRO = ELEMENTS.register(
            "electro", () -> feedback(new GenshinElement(false, false, "elemental.gim.electro"),
                    0xB388FF, ParticleTypes.ELECTRIC_SPARK));

    public static final DeferredHolder<GenshinElement, GenshinElement> DENDRO = ELEMENTS.register(
            "dendro", () -> feedback(new GenshinElement(false, false, "elemental.gim.dendro"),
                    0x9CCC65, ParticleTypes.HAPPY_VILLAGER));

    /** 冰 —— 只负责附着本身；「寒冷」减速由 {@link ColdElement}（寒）承载。 */
    public static final DeferredHolder<GenshinElement, GenshinElement> CYRO = ELEMENTS.register(
            "cyro", () -> feedback(new GenshinElement(false, false, "elemental.gim.cyro"),
                    0x99FFFF, ParticleTypes.SNOWFLAKE));

    public static final DeferredHolder<GenshinElement, GenshinElement> GEO = ELEMENTS.register(
            "geo", () -> feedback(new GenshinElement(false, true, "elemental.gim.geo"),
                    0xFFD54F, ParticleTypes.CRIT));

    // ======== 类元素（关联主元素，注册后调用 setupSubElements 设置） ========
    /** 冻 —— 冻结反应生成物，只负责附着本身；「禁 AI」由 {@link ColdElement}（寒）承载。 */
    public static final DeferredHolder<GenshinElement, GenshinElement> FROZEN = ELEMENTS.register(
            "frozen", () -> feedback(new GenshinElement(false, false, "elemental.gim.frozen"),
                    0x99FFFF, ParticleTypes.SNOWFLAKE));

    /**
     * <b>寒</b> —— 冰/冻的附加效果载体（减速、禁 AI）。
     *
     * <p>刻意<b>不</b>做成「mainElement 归并到冰」的类元素：{@code mainElement} 的唯一含义是
     * 「参与反应配对时并入主元素」，归并进去寒就会被当成冰消耗/扩散。独立注册后没有任何反应以寒为配对方，
     * 天然不参与反应；伴随关系（有冰/冻就有寒）由 {@code ColdAura} 每 tick 同步。
     */
    public static final DeferredHolder<GenshinElement, ColdElement> COLD = ELEMENTS.register(
            "cold", () -> feedback(new ColdElement("elemental.gim.cold"),
                    0x99FFFF, ParticleTypes.SNOWFLAKE));

    public static final DeferredHolder<GenshinElement, GenshinElement> AGGRAVATE = ELEMENTS.register(
            "aggravate", () -> feedback(new GenshinElement(true, false, "elemental.gim.aggravate"),
                    0xB388FF, ParticleTypes.ELECTRIC_SPARK));

    public static final DeferredHolder<GenshinElement, GenshinElement> BURNING = ELEMENTS.register(
            "burning", () -> feedback(new GenshinElement(true, false, "elemental.gim.burning"),
                    0xFF7043, ParticleTypes.FLAME));

    public static final DeferredHolder<GenshinElement, GenshinElement> WOOD = ELEMENTS.register(
            "wood", () -> feedback(new GenshinElement(false, false, "elemental.gim.wood"),
                    0x9CCC65, ParticleTypes.HAPPY_VILLAGER));

    /**
     * 注册示范元素并顺带指定它的反应反馈外观（颜色 + 粒子）。
     *
     * <p>反馈外观存在元素自身（{@link GenshinElement#setFeedback}），反馈出口
     * {@link com.linweiyun.elementlib.core.system.reaction.ReactionFeedback} 只做通用读取，
     * 不再认识任何具体元素实例。</p>
     */
    private static <T extends GenshinElement> T feedback(T element, int color, ParticleOptions particle) {
        element.setFeedback(color, particle);
        return element;
    }

    private ModElements() {
    }

    /**
     * 取元素实例；holder 未注册时返回 {@code null}。
     *
     * <p>演示元素未启用时全部元素都是未注册状态，参数位置拿到 {@code null} 必须跳过或返回。
     */
    @Nullable
    public static GenshinElement of(@Nullable DeferredHolder<GenshinElement, ? extends GenshinElement> holder) {
        if (holder == null || !holder.isBound()) {
            return null;
        }
        return holder.get();
    }

    /**
     * null 安全比较：holder 未注册或 element 为 {@code null} 时恒为 {@code false}。
     *
     * <p>绝不能退化成 {@code element == null} 成立。
     */
    public static boolean is(@Nullable GenshinElement element,
                             @Nullable DeferredHolder<GenshinElement, ? extends GenshinElement> holder) {
        if (element == null) {
            return false;
        }
        GenshinElement target = of(holder);
        return target != null && element == target;
    }

    /**
     * 在所有元素注册完成后调用，设置类元素的主元素关联。
     *
     * <p>寒不在其中：寒是独立元素（效果载体），不并入冰参与反应配对 —— 理由见 {@link #COLD}。
     */
    public static void setupSubElements() {
        if (!ElementLibConfig.demoElementsEnabled()) {
            return;
        }
        GenshinElement frozen = of(FROZEN);
        GenshinElement cyro = of(CYRO);
        GenshinElement aggravate = of(AGGRAVATE);
        GenshinElement electro = of(ELECTRO);
        GenshinElement burning = of(BURNING);
        GenshinElement pyro = of(PYRO);
        GenshinElement wood = of(WOOD);
        GenshinElement dendro = of(DENDRO);

        if (frozen != null && cyro != null) frozen.setMainElement(cyro);
        if (aggravate != null && electro != null) aggravate.setMainElement(electro);
        if (burning != null && pyro != null) burning.setMainElement(pyro);
        if (wood != null && dendro != null) wood.setMainElement(dendro);
    }

    /** 注册示范元素；{@code register-demo-elements=false} 时什么都不注册。 */
    public static void register(IEventBus eventBus) {
        if (!ElementLibConfig.demoElementsEnabled()) {
            LOGGER.info("[ElementLib] 示范元素未启用，跳过元素注册");
            return;
        }
        ELEMENTS.register(eventBus);
    }
}