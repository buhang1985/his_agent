package com.hisagent.cache;

import java.util.concurrent.TimeUnit;

/**
 * 缓存过期时间常量
 * 所有缓存必须设置明确的过期时间，禁止永久缓存
 */
public final class CacheTTL {
    
    private CacheTTL() {
        // 工具类，禁止实例化
    }
    
    /**
     * 患者数据缓存时间：30 分钟
     * 频繁访问，隐私敏感
     */
    public static final long PATIENT_DATA = 30;
    public static final TimeUnit PATIENT_DATA_UNIT = TimeUnit.MINUTES;
    
    /**
     * 用户会话缓存时间：2 小时
     * 与 Token 有效期一致
     */
    public static final long USER_SESSION = 2;
    public static final TimeUnit USER_SESSION_UNIT = TimeUnit.HOURS;
    
    /**
     * 医学词库缓存时间：24 小时
     * 极少变化，访问频繁
     */
    public static final long MEDICAL_TERM = 24;
    public static final TimeUnit MEDICAL_TERM_UNIT = TimeUnit.HOURS;
    
    /**
     * 配置数据缓存时间：1 小时
     * 可能动态调整
     */
    public static final long CONFIG_DATA = 1;
    public static final TimeUnit CONFIG_DATA_UNIT = TimeUnit.HOURS;
    
    /**
     * API 响应缓存时间：10 分钟
     * 短期缓存
     */
    public static final long API_RESPONSE = 10;
    public static final TimeUnit API_RESPONSE_UNIT = TimeUnit.MINUTES;
    
    /**
     * 空值缓存时间：5 分钟
     * 穿透防护
     */
    public static final long NULL_VALUE = 5;
    public static final TimeUnit NULL_VALUE_UNIT = TimeUnit.MINUTES;
    
    /**
     * 录音数据缓存时间：1 小时
     * 病历生成后自动删除
     */
    public static final long RECORDING = 1;
    public static final TimeUnit RECORDING_UNIT = TimeUnit.HOURS;
}
