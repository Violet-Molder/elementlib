package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.element.GenshinElement;
import org.jetbrains.annotations.Nullable;

/**
 * 元素解析器 —— 当 {@link ElibAttackAction#element()} 为 {@code null} 时，由它决定这次攻击带什么元素。
 *
 * <p>elementlib 默认实现读主手的元素剑；MineGenshin 会换成「出战角色元素」。
 */
@FunctionalInterface
public interface ElibAttackElementResolver {

    @Nullable
    GenshinElement elementOf(ElibAttackAction action);
}
