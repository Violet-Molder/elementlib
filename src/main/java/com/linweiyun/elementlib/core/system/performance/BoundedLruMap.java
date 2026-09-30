package com.linweiyun.elementlib.core.system.performance;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 有上限的 LRU 表（计算优化模块）。
 */
public final class BoundedLruMap {

    /** 默认上限：够放「当前场景所有相关实体 + 最近打过的一批」 */
    public static final int DEFAULT_MAX_ENTRIES = 4096;

    private BoundedLruMap() {}

    /**
     * 建一张带访问顺序的有上限表：get / put 都会把键挪到「最近使用」一端，
     * 超出上限时淘汰最久没被碰过的键。
     *
     * @param maxEntries 上限；小于 1 时按 1 处理
     */
    public static <K, V> Map<K, V> create(int maxEntries) {
        int limit = Math.max(1, maxEntries);
        return new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    public static <K, V> Map<K, V> create() {
        return create(DEFAULT_MAX_ENTRIES);
    }
}
