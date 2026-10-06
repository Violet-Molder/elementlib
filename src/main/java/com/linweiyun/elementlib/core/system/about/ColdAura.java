package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.ColdElement;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.api.ElementRoles;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>寒元素的伴随机制</b> —— 「寒往往伴随冰/冻存在」这句规则的落点。
 */
public final class ColdAura {
    private static final AttachmentProfile PROFILE = AttachmentProfile.permanent(1.0f);

    private ColdAura() {
    }
    public static boolean tick(LivingEntity entity, StatusContainer container) {
        if (entity == null || container == null || container.isEmpty()) {
            return false;
        }

        boolean cryoFamily = false;
        boolean frozen = false;
        boolean cold = false;
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (!(inst instanceof ElementalAttachmentInstance ea)) continue;
            GenshinElement element = ea.getElement();
            if (element == null) continue;
            if (ElementRoles.is(element, ElementRoles.FROZEN)) {
                frozen = true;
                cryoFamily = true;
            } else if (ElementRoles.is(element, ElementRoles.CYRO)) {
                cryoFamily = true;
            } else if (ElementRoles.is(element, ElementRoles.COLD)) {
                cold = true;
            }
        }
        if (!cryoFamily && !cold) {
            return false;
        }

        if (cryoFamily && !cold) {
            GenshinElement coldElement = ElementRoles.of(ElementRoles.COLD);
            if (coldElement != null) {
                ElementalHost host = EntityHost.of(entity);
                cold = ElementalAttachmentHelper
                        .attachInternal(host, coldElement, AttachmentSource.SPECIAL, PROFILE)
                        .attached();
            }
        } else if (!cryoFamily && cold) {
            container.removeFirst(ColdAura::isCold);
            cold = false;
        }

        ColdElement.applyEffects(entity, cold, cryoFamily, frozen);
        return cold && frozen;
    }

    private static boolean isCold(@Nullable StatusInstance inst) {
        return inst instanceof ElementalAttachmentInstance ea
                && ElementRoles.is(ea.getElement(), ElementRoles.COLD);
    }
}
