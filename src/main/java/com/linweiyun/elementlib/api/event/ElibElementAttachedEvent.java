package com.linweiyun.elementlib.api.event;

import com.linweiyun.elementlib.ElementLib;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachWrite;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * 元素附着成功事件 —— 附着真的写进宿主容器时广播，被宿主拒收时不发。
 *
 * <p>{@link #source()} 表达覆盖规则，{@link #originId()} 表达来源（哪个环境、哪一招），
 * {@link #sourceKey()} 表达角色；{@link #profile()} 与 {@link #write()} 描述这一次附着本身。
 * 只读通知，不可取消。
 */
public final class ElibElementAttachedEvent extends Event implements ElibIdentifiedEvent {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(ElementLib.MOD_ID, "element_attached");

    @Nullable private final ServerLevel level;
    private final long gameTime;
    @Nullable private final ElementalHost host;
    private final GenshinElement element;
    private final AttachmentSource source;
    private final AttachmentProfile profile;
    private final float actualQuantity;
    private final AttachWrite write;
    @Nullable private final Identifier originId;
    @Nullable private final String sourceKey;
    @Nullable private final Entity sourceEntity;
    private final boolean fromReaction;

    public ElibElementAttachedEvent(@Nullable ServerLevel level, long gameTime,
                                    @Nullable ElementalHost host, GenshinElement element,
                                    AttachmentSource source, AttachmentProfile profile,
                                    float actualQuantity, AttachWrite write,
                                    @Nullable Identifier originId, @Nullable String sourceKey,
                                    @Nullable Entity sourceEntity, boolean fromReaction) {
        this.level = level;
        this.gameTime = gameTime;
        this.host = host;
        this.element = element;
        this.source = source;
        this.profile = profile;
        this.actualQuantity = actualQuantity;
        this.write = write;
        this.originId = originId;
        this.sourceKey = sourceKey;
        this.sourceEntity = sourceEntity;
        this.fromReaction = fromReaction;
    }

    @Override
    public Identifier eventId() {
        return ID;
    }

    @Override
    public String displayName() {
        return "元素附着";
    }

    /** 事件发生的世界；宿主既不是方块也没有实体时可能为 {@code null}。 */
    @Nullable
    public ServerLevel level() {
        return level;
    }

    /** 附着时刻；来源没有记录时刻时为 {@code 0}。 */
    public long gameTime() {
        return gameTime;
    }

    /** 被附着的宿主（生物 / 方块 / 出战角色）；容器直连写入时为 {@code null}。 */
    @Nullable
    public ElementalHost host() {
        return host;
    }

    /** 宿主标识，便于日志；没有宿主时为 {@code null}。 */
    @Nullable
    public String hostKey() {
        return host == null ? null : host.hostKey();
    }

    /** 挂在宿主身上的是哪个元素。 */
    public GenshinElement element() {
        return element;
    }

    /** 附着来源的<b>覆盖规则</b>轴。 */
    public AttachmentSource source() {
        return source;
    }

    /** 附着参数（强弱档次、损耗、衰减、时长）。 */
    public AttachmentProfile profile() {
        return profile;
    }

    /** 实际写入量 = {@code baseQuantity × lossMultiplier}。 */
    public float actualQuantity() {
        return actualQuantity;
    }

    /** 这一次相对已有附着做了什么。 */
    public AttachWrite write() {
        return write;
    }

    /** 来源标识：什么环境、哪一招（可空）。 */
    @Nullable
    public Identifier originId() {
        return originId;
    }

    /** 角色身份键（可空）。 */
    @Nullable
    public String sourceKey() {
        return sourceKey;
    }

    /** 挂元素的实体（可空）。 */
    @Nullable
    public Entity sourceEntity() {
        return sourceEntity;
    }

    /**
     * 这一份附着是不是<b>反应内部的二次写入</b>（冻结写冻元素、扩散写新目标）。
     *
     * <p>为真时它照发，但监听者不要把「反应的后果」当成「玩家又打了一下」。
     */
    public boolean fromReaction() {
        return fromReaction;
    }

    @Override
    public String toString() {
        return "ElibElementAttachedEvent[" + element.getId() + " write=" + write
                + " source=" + source + " origin=" + originId + " qty=" + actualQuantity
                + " host=" + hostKey() + "]";
    }
}