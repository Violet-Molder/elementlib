package com.linweiyun.elementlib.core.status;

import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;

/**
 * 叠加实例基类 —— 可以被存进 StatusContainer 的东西的统一规格。
 */
public class StatusInstance implements IPersistedSerializable {

    /** 序列化恢复用的类型标识，子类构造时设置 */
    @Persisted(key = "type_id")
    protected String typeId = "";

    /** 子类自己决定返回什么字符串 */
    public String getTypeId() { return typeId; }

    /** 每次宿主 tick 时调用，让生命周期流逝（倒计时或量值衰减） */
    public void tick(){};

    /** 是否已经结束（该被移除了） */
    public boolean isFinished() {
        return false;
    };

    /** 被容器移除时的回调钩子（自然结束或强制清除都会调），默认空实现 */
    public void onRemove() {}

    /** 深拷贝 */
    public StatusInstance copy() {
        return this;
    }
}
