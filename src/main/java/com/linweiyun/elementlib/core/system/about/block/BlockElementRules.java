package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementRoles;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * <b>「这个方块能被什么附着」的注册表</b>。
 * <p>新增一种「与元素有关的方块」时，不需要改这里的判断逻辑，而是注册一条规则：
 * <p>规则按注册顺序匹配，先命中先返回。内置的水与冰族规则在静态块里注册，且只在示范元素启用时注册。
 */
public final class BlockElementRules {

    /** 一条规则：给定方块状态与来袭元素，回答收不收。 */
    @FunctionalInterface
    public interface Rule {
        boolean accepts(BlockState state, GenshinElement element);
    }

    private record Entry(Predicate<BlockState> matcher, Rule rule, String name) {}

    private static final List<Entry> RULES = new ArrayList<>();

    private BlockElementRules() {
    }

    /**
     * 注册一条规则。
     *
     * @param matcher 匹配哪些方块状态
     * @param rule    匹配上之后，能不能接收这个元素
     * @param name    规则名（仅用于排查）
     */
    public static void register(Predicate<BlockState> matcher, Rule rule, String name) {
        RULES.add(new Entry(matcher, rule, name));
    }

    /** 这个方块状态收不收这个元素。没有任何规则命中 → 不收。 */
    public static boolean accepts(BlockState state, GenshinElement element) {
        if (state == null || element == null) {
            return false;
        }
        for (Entry entry : RULES) {
            if (entry.matcher().test(state)) {
                return entry.rule().accepts(state, element);
            }
        }
        return false;
    }

    /** 已登记的规则条数（诊断用）。 */
    public static int size() {
        return RULES.size();
    }

    // ==================== 内置：水与冰族 ====================

    static {
        register(state -> isSourceWater(state),
                (state, element) -> ElementRoles.is(element, ElementRoles.CYRO)
                        || ElementRoles.is(element, ElementRoles.FROZEN),
                "minecraft:water");
        register(state -> isIceFamily(state),
                (state, element) -> ElementRoles.is(element, ElementRoles.PYRO)
                        || ElementRoles.is(element, ElementRoles.CYRO)
                        || ElementRoles.is(element, ElementRoles.FROZEN),
                "minecraft:ice_family");
    }

    /** 完整水源（level=0）：水方块、且不是流动水。 */
    public static boolean isSourceWater(BlockState state) {
        return state.is(Blocks.WATER)
                && state.getValue(BlockStateProperties.LEVEL) == 0;
    }

    /** 浮冰 / 冰 / 浮冰砖 / 蓝冰 —— 冰族方块。 */
    public static boolean isIceFamily(BlockState state) {
        return state.is(Blocks.FROSTED_ICE) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    /**
     * <b>这个方块自带什么元素</b>（环境自附着）—— 不衰减、一直挂在自己身上，
     * 被反应消耗完后由 {@link BlockSelfAura} 周期补回。
     *
     * <p>这是「冰相当于一直给自己挂冰、水一直给自己挂水」的落点：反应需要先手元素，
     * 水打冰能冻结是因为水这边自带水，火打冰能融化是因为冰这边自带冰。
     *
     * @return 自带元素；没有（或示范元素未启用）则 {@code null}
     */
    @Nullable
    public static GenshinElement selfAura(BlockState state) {
        if (state == null) {
            return null;
        }
        if (isSourceWater(state)) {
            return ElementRoles.of(ElementRoles.HYDRO);
        }
        // 浮冰不额外挂冰气场：它的元素是冻结反应写进去的冻；给它挂一份永久冰会让
        // 「冰族没有冰/冻就化水」这条迁移规则永远不成立，浮冰永不化。
        if (state.is(Blocks.FROSTED_ICE)) {
            return null;
        }
        if (isIceFamily(state)) {
            return ElementRoles.of(ElementRoles.CYRO);
        }
        return null;
    }
}
