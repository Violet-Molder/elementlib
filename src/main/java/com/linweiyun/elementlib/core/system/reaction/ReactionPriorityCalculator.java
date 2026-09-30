package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 反应优先级 —— 决定同一份先手元素面对多个后手元素时，先和谁反应。
 */
public final class ReactionPriorityCalculator {

    /** 未登记在默认顺序表里的主元素统一排到最后（比表内任何下标都大）。 */
    public static final int UNKNOWN_PRIORITY = 50;

    /**
     * 默认反应优先级顺序 —— 唯一真相。
     *
     * <p>后手元素与各先手元素反应时，按先手元素（主元素）在本表里的下标从小到大排序；
     * 索引越靠前的先手元素越先被反应消耗。
     */
    private static final List<DeferredHolder<GenshinElement, ? extends GenshinElement>> DEFAULT_ORDER = List.of(
            ModElements.ANEMO, ModElements.CYRO, ModElements.ELECTRO,
            ModElements.HYDRO, ModElements.FROZEN, ModElements.PYRO,
            ModElements.DENDRO, ModElements.AGGRAVATE, ModElements.GEO);

    private ReactionPriorityCalculator() {
    }

    /**
     * 算一个反应的具体优先级 —— 只看先手（目标身上已有）元素的主元素排在顺序表里的位置。
     *
     * <p>注册时手填了非负 {@code basePriority} 的反应不走这里；只有填 -1（表示「用默认顺序」）的反应才会调用。
     *
     * @param defenderElement 先手附着的元素（目标身上已有的那一个），可以为 {@code null}
     */
    public static int computeFor(@Nullable GenshinElement defenderElement) {
        if (defenderElement == null) {
            return UNKNOWN_PRIORITY;
        }
        return priorityOf(defenderElement.getMainElement());
    }

    /** 主元素在默认顺序表里的下标；不在表内（含 {@code null}）返回 {@link #UNKNOWN_PRIORITY}。 */
    public static int priorityOf(@Nullable GenshinElement mainElement) {
        if (mainElement == null) {
            return UNKNOWN_PRIORITY;
        }
        for (int i = 0; i < DEFAULT_ORDER.size(); i++) {
            if (ModElements.is(mainElement, DEFAULT_ORDER.get(i))) return i;
        }
        return UNKNOWN_PRIORITY;
    }

    public static boolean hasFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ModElements.is(ea.getElement(), ModElements.FROZEN)) return true;
        }
        return false;
    }

    public static boolean hasAggravate(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ModElements.is(ea.getElement(), ModElements.AGGRAVATE)) return true;
        }
        return false;
    }
}
