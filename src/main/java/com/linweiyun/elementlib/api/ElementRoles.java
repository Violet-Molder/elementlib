package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元素角色表 —— 框架代码只认「角色」，不认具体元素实例。
 *
 * <p>绑定关系由元素注册方（示范元素或接入方）在元素注册完成后通过 {@link #bind} 建立；
 * 未绑定的角色一律解析为 {@code null}，框架必须跳过而不是崩。
 */
public final class ElementRoles {

    public static final String FYSIKOS = "fysikos";
    public static final String PYRO = "pyro";
    public static final String HYDRO = "hydro";
    public static final String ANEMO = "anemo";
    public static final String ELECTRO = "electro";
    public static final String DENDRO = "dendro";
    public static final String CYRO = "cyro";
    public static final String GEO = "geo";
    public static final String FROZEN = "frozen";
    public static final String COLD = "cold";
    public static final String AGGRAVATE = "aggravate";
    public static final String BURNING = "burning";
    public static final String WOOD = "wood";

    private static final Map<String, GenshinElement> BOUND = new ConcurrentHashMap<>();

    private ElementRoles() {
    }

    public static void bind(String role, @Nullable GenshinElement element) {
        if (role == null) {
            return;
        }
        if (element == null) {
            BOUND.remove(role);
        } else {
            BOUND.put(role, element);
        }
    }

    @Nullable
    public static GenshinElement of(@Nullable String role) {
        return role == null ? null : BOUND.get(role);
    }

    public static boolean is(@Nullable GenshinElement element, @Nullable String role) {
        if (element == null || role == null) {
            return false;
        }
        return BOUND.get(role) == element;
    }

    public static Set<String> boundRoles() {
        return Set.copyOf(BOUND.keySet());
    }
}
