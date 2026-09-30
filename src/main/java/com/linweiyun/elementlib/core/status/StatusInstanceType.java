package com.linweiyun.elementlib.core.status;

import java.util.function.Supplier;

/**
 * StatusInstance 子类的注册表条目：typeId 与构造器。
 */
public record StatusInstanceType<T extends StatusInstance>(
        String typeId,
        Supplier<T> constructor
) {}
