package com.linweiyun.elementlib.core.system.about.host;

import com.linweiyun.elementlib.core.attachment.ElementalAttachments;
import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.module.ElibModuleHosts;
import com.linweiyun.elementlib.core.module.ElibModuleTypes;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachable;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>生物宿主</b> —— {@link ElementalHost} 在 {@link LivingEntity} 上的实现。
 */
public final class EntityHost implements ElementalHost {

    private final LivingEntity entity;

    private EntityHost(LivingEntity entity) {
        this.entity = entity;
    }

    /** 包一个生物实体；{@code entity == null} 时返回 {@code null}。 */
    @Nullable
    public static EntityHost of(@Nullable LivingEntity entity) {
        return entity == null ? null : new EntityHost(entity);
    }

    @Override
    public boolean isValid() {
        return entity.isAlive() && !entity.isRemoved();
    }

    @Override
    public StatusContainer container() {
        if (!isValid()) {
            return null;
        }
        var host = ElibModuleHosts.of(entity);
        return host == null ? null : host.ensure(ElibModuleTypes.ELEMENT);
    }

    @Override
    public String hostKey() {
        return "entity:" + entity.getUUID();
    }

    @Override
    public boolean acceptsElement(GenshinElement element, AttachmentSource source,
                                  AttachmentProfile profile) {
        if (!(entity instanceof ElementalAttachable attachable)) {
            return true;
        }
        return attachable.onAttachElement(element, source, profile);
    }

    @Override
    public boolean acceptsReaction(GenshinElement attackerElement, GenshinElement defenderElement,
                                   com.linweiyun.elementlib.api.ElementalReactionType reactionType) {
        if (!(entity instanceof ElementalAttachable attachable)) {
            return true;
        }
        return attachable.onReactElement(attackerElement, defenderElement, reactionType);
    }

    @Override
    public void onElementAttached(GenshinElement element) {
        element.onAttach(this);
    }

    @Override
    public void onElementDetached(GenshinElement element) {
        element.onDetach(this);
    }

    @Override
    public LivingEntity entity() {
        return entity;
    }

    @Override
    public String toString() {
        return hostKey();
    }
}
