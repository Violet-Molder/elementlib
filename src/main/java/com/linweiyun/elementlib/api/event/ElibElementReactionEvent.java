package com.linweiyun.elementlib.api.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.api.ElementalReactionType;
import com.linweiyun.elementlib.api.ReactionCategory;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * 元素反应事件 —— 每一个真的成立的反应各广播一条。
 *
 * <p>方块上不出飘字的反应（{@code showsIndicator == false}）也照常广播。
 */
public final class ElibElementReactionEvent extends Event implements ElibIdentifiedEvent {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "element_reacted");

    @Nullable private final ServerLevel level;
    private final long gameTime;
    @Nullable private final ElementalHost host;
    private final ElementalReactionType type;
    private final ReactionCategory category;
    private final GenshinElement triggerElement;
    @Nullable private final GenshinElement auraElement;
    private final float reactionUnit;
    private final AttachmentSource source;
    @Nullable private final Identifier originId;
    @Nullable private final String sourceKey;
    @Nullable private final Entity sourceEntity;
    @Nullable private final ReactionResult result;

    public ElibElementReactionEvent(@Nullable ServerLevel level, long gameTime,
                                    @Nullable ElementalHost host, ElementalReactionType type,
                                    ReactionCategory category, GenshinElement triggerElement,
                                    @Nullable GenshinElement auraElement, float reactionUnit,
                                    AttachmentSource source, @Nullable Identifier originId,
                                    @Nullable String sourceKey, @Nullable Entity sourceEntity,
                                    @Nullable ReactionResult result) {
        this.level = level;
        this.gameTime = gameTime;
        this.host = host;
        this.type = type;
        this.category = category;
        this.triggerElement = triggerElement;
        this.auraElement = auraElement;
        this.reactionUnit = reactionUnit;
        this.source = source;
        this.originId = originId;
        this.sourceKey = sourceKey;
        this.sourceEntity = sourceEntity;
        this.result = result;
    }

    @Override
    public Identifier eventId() {
        return ID;
    }

    @Override
    public String displayName() {
        return "元素反应";
    }

    @Nullable
    public ServerLevel level() {
        return level;
    }

    public long gameTime() {
        return gameTime;
    }

    @Nullable
    public ElementalHost host() {
        return host;
    }

    @Nullable
    public String hostKey() {
        return host == null ? null : host.hostKey();
    }

    /** 反应类型（蒸发 / 融化 / 冻结 / 超导 / 扩散 / 月感电 …）。 */
    public ElementalReactionType type() {
        return type;
    }

    /** 反应大类（增幅 / 剧变 / 状态 / 月曜 / 星烁）。 */
    public ReactionCategory category() {
        return category;
    }

    /** 后手：触发这次反应的元素。 */
    public GenshinElement triggerElement() {
        return triggerElement;
    }

    /** 先手：被这次反应消耗掉的那个元素；纯自身反应等场景可能为 {@code null}。 */
    @Nullable
    public GenshinElement auraElement() {
        return auraElement;
    }

    /** 触发反应时使用的元素量。 */
    public float reactionUnit() {
        return reactionUnit;
    }

    public AttachmentSource source() {
        return source;
    }

    @Nullable
    public Identifier originId() {
        return originId;
    }

    @Nullable
    public String sourceKey() {
        return sourceKey;
    }

    @Nullable
    public Entity sourceEntity() {
        return sourceEntity;
    }

    /** 这一次反应的结果（消耗量、增幅倍率等）。 */
    @Nullable
    public ReactionResult result() {
        return result;
    }

    @Override
    public String toString() {
        return "ElibElementReactionEvent[" + type + " trigger="
                + (triggerElement == null ? "-" : triggerElement.getId())
                + " aura=" + (auraElement == null ? "-" : auraElement.getId())
                + " host=" + hostKey() + "]";
    }
}