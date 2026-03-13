package com.hisagent.annotation;

/**
 * 脱敏类型枚举
 */
public enum MaskType {
    /**
     * 手机号
     */
    PHONE,
    
    /**
     * 身份证号
     */
    ID_CARD,
    
    /**
     * 姓名
     */
    NAME,
    
    /**
     * 邮箱
     */
    EMAIL,
    
    /**
     * 地址
     */
    ADDRESS,
    
    /**
     * 自定义（保留前后缀）
     */
    CUSTOM
}
