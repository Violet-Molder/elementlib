package com.linweiyun.elementlib.core.system.reaction;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.api.ElementRoles;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 反应优先级 —— 决定同一份先手元素面对多个后手元素时，先和谁反应。
 */
public final class ReactionPriorityCalculator {

    /** 未登记在默认顺序表里的主元素统一排到最后（比表内任何下标都大）。 */
    public static final int UNKNOWN_PRIORITY = 50;

    private static final List<String> DEFAULT_ORDER = List.of(
            ElementRoles.ANEMO, ElementRoles.CYRO, ElementRoles.ELECTRO,
            ElementRoles.HYDRO, ElementRoles.FROZEN, ElementRoles.PYRO,
            ElementRoles.DENDRO, ElementRoles.AGGRAVATE, ElementRoles.GEO);

    private ReactionPriorityCalculator() {
    }

    public static int computeFor(@Nullable GenshinElement defenderElement) {
        if (defenderElement == null) {
            return UNKNOWN_PRIORITY;
        }
        return priorityOf(defenderElement.getMainElement());
    }
    public static int priorityOf(@Nullable GenshinElement mainElement) {
        if (mainElement == null) {
            return UNKNOWN_PRIORITY;
        }
        for (int i = 0; i < DEFAULT_ORDER.size(); i++) {
            if (ElementRoles.is(mainElement, DEFAULT_ORDER.get(i))) return i;
        }
        return UNKNOWN_PRIORITY;
    }

    public static boolean hasFrozen(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ElementRoles.is(ea.getElement(), ElementRoles.FROZEN)) return true;
        }
        return false;
    }

    public static boolean hasAggravate(StatusContainer container) {
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea
                    && ElementRoles.is(ea.getElement(), ElementRoles.AGGRAVATE)) return true;
        }
        return false;
    }
}
