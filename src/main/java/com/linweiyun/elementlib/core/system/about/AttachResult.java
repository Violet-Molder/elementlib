package com.linweiyun.elementlib.core.system.about;

import com.linweiyun.elementlib.core.system.reaction.ReactionResult;
import org.jetbrains.annotations.Nullable;

/**
 * 一次附着的结果 —— 「宿主收没收这次附着」+「附着之后触发了什么反应」。
 *
 * @param attached 宿主是否接受了这次附着
 * @param reaction 附着之后触发的反应（没发生则 {@code null}）
 */
public record AttachResult(boolean attached, @Nullable ReactionResult reaction) {

    /** 被宿主拒收（或宿主无效）—— 元素没上去，也不该有任何反应。 */
    public static final AttachResult REJECTED = new AttachResult(false, null);

    /** 挂上了，但没有任何反应发生。 */
    public static final AttachResult ATTACHED_NO_REACTION = new AttachResult(true, null);

    /** 有没有真的发生反应。 */
    public boolean reacted() {
        return attached && reaction != null && reaction.isReacted();
    }
}
