package com.linweiyun.elementlib.api;

import com.linweiyun.elementlib.core.system.registry.ModRegistries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * 元素反应类型 —— 注册在 {@code elementlib:reaction_types} 注册表里的对象。
 *
 * <p>实例自身不持有 id：id 就是它在反应类型注册表里的 key，注册之后由 {@link #getId()} 查询。
 * 使用者可以注册自己的反应类型，再用它构造自己的 {@code ElementalReaction}。
 */
public final class ElementalReactionType {

    private final String translationKey;
    private final ReactionCategory category;

    public ElementalReactionType(String translationKey, ReactionCategory category) {
        this.translationKey = translationKey;
        this.category = category;
    }

    /** 注册表 key；实例尚未注册时为 {@code null}。 */
    @Nullable
    public Identifier getId() {
        return ModRegistries.REACTION_TYPE_REGISTRY.getKey(this);
    }

    /** 注册表 key 的 path；尚未注册时为 {@code ""}。 */
    public String getPath() {
        Identifier id = getId();
        return id != null ? id.getPath() : "";
    }

    /** 翻译键，例如 {@code reaction.minegenshin.melt}。 */
    public String getTranslationKey() {
        return translationKey;
    }

    public ReactionCategory getCategory() {
        return category;
    }

    public boolean is(ReactionCategory category) {
        return this.category == category;
    }

    @Override
    public String toString() {
        return getPath();
    }
}
