package com.linweiyun.elementlib.core.system.about.block;

import com.linweiyun.elementlib.core.attachment.StatusContainer;
import com.linweiyun.elementlib.core.element.GenshinElement;
import com.linweiyun.elementlib.core.status.StatusInstance;
import com.linweiyun.elementlib.core.system.about.AttachmentProfile;
import com.linweiyun.elementlib.core.system.about.AttachmentSource;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentHelper;
import com.linweiyun.elementlib.core.system.about.ElementalAttachmentInstance;
import com.linweiyun.elementlib.core.system.about.host.BlockHost;
import net.minecraft.world.level.block.state.BlockState;

/**
 * <b>方块的环境自附着</b> —— 「冰一直给自己挂冰、水一直给自己挂水」。
 */
public final class BlockSelfAura {

    /** 自带元素的满量（U）。取 1U：正好够一次 1:1 的反应把它换掉（水+冰=冻结）。 */
    private static final float BASE_QUANTITY = 1.0f;

    private BlockSelfAura() {
    }

    /**
     * 确保这个方块的容器里有它自带的元素。
     */
    public static void ensure(BlockHost host, StatusContainer container, BlockState state) {
        if (host == null || container == null) {
            return;
        }
        GenshinElement aura = BlockElementRules.selfAura(state);
        if (aura == null) {
            return;
        }
        if (hasAlive(container, aura)) {
            return;
        }
        // host 传 null：自带元素是方块的天性，不经过「能被什么附着」那道外部筛查
        //（水本来就不收水，但水当然自带水）。用现成的容器写入，不回头问 host.container()，以免递归。
        ElementalAttachmentHelper.attachInternalTo(container, null, aura,
                AttachmentSource.ENVIRONMENTAL, AttachmentProfile.permanent(BASE_QUANTITY));
    }

    /** 这个方块现在有没有自带元素（只读，用于迁移判断与诊断）。 */
    public static boolean hasAlive(StatusContainer container, GenshinElement element) {
        if (container == null || element == null) {
            return false;
        }
        for (StatusInstance inst : container.getAll()) {
            if (inst.isFinished()) continue;
            if (inst instanceof ElementalAttachmentInstance ea && ea.getElement() == element) {
                return true;
            }
        }
        return false;
    }
}
