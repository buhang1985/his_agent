package com.hisagent.cache;

/**
 * 缓存常量定义
 */
public final class CacheConstants {

    private CacheConstants() {
        // 工具类，禁止实例化
    }

    /**
     * 空值缓存标记
     * 用于缓存穿透防护 - 当数据库查询结果为空时，缓存此标记
     */
    public static final String NULL_VALUE_MARKER = "__NULL__CACHE__VALUE__";

    /**
     * 缓存前缀
     */
    public static final String CACHE_PREFIX = "his_agent";

    /**
     * 布隆过滤器误判率
     */
    public static final double BLOOM_FILTER_FPP = 0.01;

    /**
     * 缓存删除最大重试次数
     */
    public static final int MAX_DELETE_RETRY = 3;

    /**
     * 缓存删除重试间隔（毫秒）
     */
    public static final long DELETE_RETRY_INTERVAL_MS = 1000;

    /**
     * 热点数据刷新提前时间（分钟）
     * 在缓存过期前此时间开始刷新，避免热点数据失效
     */
    public static final long HOT_DATA_REFRESH_AHEAD_MINUTES = 5;
}
