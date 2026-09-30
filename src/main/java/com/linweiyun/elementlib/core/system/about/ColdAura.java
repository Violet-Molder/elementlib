package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.ColdElement;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.element.ModElements;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.host.ElementalHost;
import com.linweiyun.elementlib.core.system.about.host.EntityHost;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * <b>寒元素的伴随机制</b> —— 「寒往往伴随冰/冻存在」这句规则的落点。
 *
 * <pre>
 *   容器里有活的 冰 或 冻  → 补一条寒（走宿主筛查，冰史莱姆在这一步被挡住）
 *   冰和冻都没了           → 寒一起走
 *   然后按「有寒 + 有冰族 / 有冻」应用或撤销减速与禁 AI
 * </pre>
 *
 * <p>放在每 tick 而不是附着的 onAttach/onDetach 钩子里：附着钩子在实例入容器之前调用、
 * 移除钩子在实例出容器之前调用，钩子里读到的容器一定是旧状态。这里每 tick 读一次容器，
 * 只有一份判据，效果最多晚 1 tick 生效。
 *
 * <p>只对生物做事：方块宿主没有实体，寒也没有效果可言。
 */
public final class ColdAura {

    /** 寒的量：常驻（不衰减），被消耗或被清掉后下次同步补回。 */
    private static final AttachmentProfile PROFILE = AttachmentProfile.permanent(1.0f);

    private ColdAura() {
    }

    /**
     * 每 tick 同步一次寒，并应用效果。
     *
     * @return true 表示此刻「有寒且有冻」—— 调用方据此锁住位移（冻结期间不为水流/浮力积累速度）
     */
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
            if (ModElements.is(element, ModElements.FROZEN)) {
                frozen = true;
                cryoFamily = true;
            } else if (ModElements.is(element, ModElements.CYRO)) {
                cryoFamily = true;
            } else if (ModElements.is(element, ModElements.COLD)) {
                cold = true;
            }
        }

        // 既没有冰/冻也没有寒 —— 绝大多数生物每 tick 都走这条，什么都别做
        if (!cryoFamily && !cold) {
            return false;
        }

        if (cryoFamily && !cold) {
            // 补寒。走宿主筛查（attachInternal 只免掉「反应」，筛查照问）：
            // 元素生物在这里被挡下 —— 挂冰但不受冰影响，正是靠「不收寒」表达的。
            GenshinElement coldElement = ModElements.of(ModElements.COLD);
            if (coldElement != null) {
                ElementalHost host = EntityHost.of(entity);
                cold = ElementalAttachmentHelper
                        .attachInternal(host, coldElement, AttachmentSource.SPECIAL, PROFILE)
                        .attached();
            }
        } else if (!cryoFamily && cold) {
            // 冰和冻都没了 → 寒一起走（这里不在容器回调里改列表，不存在迭代中改动的风险）
            container.removeFirst(ColdAura::isCold);
            cold = false;
        }

        ColdElement.applyEffects(entity, cold, cryoFamily, frozen);
        return cold && frozen;
    }

    private static boolean isCold(@Nullable StatusInstance inst) {
        return inst instanceof ElementalAttachmentInstance ea
                && ModElements.is(ea.getElement(), ModElements.COLD);
    }
}
