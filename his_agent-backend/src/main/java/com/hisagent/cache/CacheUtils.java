package com.hisagent.cache;

import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 缓存工具类 - 提供缓存穿透和雪崩防护功能
 */
@Component
public class CacheUtils {

    private static final Random RANDOM = new Random();

    private CacheUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 计算带随机偏移的过期时间
     * 用于防止缓存雪崩 - 同一时间大量缓存失效
     *
     * @param baseTTL 基础过期时间
     * @param unit    时间单位
     * @param offset  偏移比例（0.0-1.0），例如 0.2 表示±20%
     * @return 实际过期时间
     */
    public static long calculateTTLWithJitter(long baseTTL, TimeUnit unit, double offset) {
        if (offset < 0 || offset > 1) {
            throw new IllegalArgumentException("Offset must be between 0.0 and 1.0");
        }

        long baseMillis = unit.toMillis(baseTTL);
        long jitterRange = (long) (baseMillis * offset);
        // 使用 nextInt 确保在范围内的随机数
        long jitter = (RANDOM.nextInt((int) Math.min(jitterRange * 2 + 1, Integer.MAX_VALUE)) - jitterRange);
        long actualMillis = baseMillis + jitter;

        // 确保至少为 1 毫秒
        return Math.max(1, actualMillis);
    }

    /**
     * 计算带随机偏移的过期时间（默认±20% 偏移）
     *
     * @param baseTTL 基础过期时间
     * @param unit    时间单位
     * @return 实际过期时间（毫秒）
     */
    public static long calculateTTLWithJitter(long baseTTL, TimeUnit unit) {
        return calculateTTLWithJitter(baseTTL, unit, 0.2);
    }

    /**
     * 判断是否为空值缓存
     * 用于缓存穿透防护 - 缓存空值避免频繁查询数据库
     *
     * @param value 缓存值
     * @return true 如果是空值标记
     */
    public static boolean isNullValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return CacheConstants.NULL_VALUE_MARKER.equals(value);
        }
        return false;
    }

    /**
     * 获取空值标记
     *
     * @return 空值标记字符串
     */
    public static String getNullValueMarker() {
        return CacheConstants.NULL_VALUE_MARKER;
    }

    /**
     * 包装值，如果是 null 则返回空值标记
     *
     * @param value 原始值
     * @return 包装后的值
     */
    public static Object wrapValue(Object value) {
        if (value == null) {
            return CacheConstants.NULL_VALUE_MARKER;
        }
        return value;
    }

    /**
     * 解包值，如果是空值标记则返回 null
     *
     * @param value 缓存值
     * @param <T>   期望的类型
     * @return 原始值或 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrapValue(Object value) {
        if (isNullValue(value)) {
            return null;
        }
        return (T) value;
    }
}
