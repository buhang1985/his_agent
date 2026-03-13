package com.hisagent.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MaskData {

    /**
     * 脱敏类型
     */
    MaskType type() default MaskType.PHONE;

    /**
     * 前缀保留长度（用于 CUSTOM 类型）
     */
    int prefixLength() default 3;

    /**
     * 后缀保留长度（用于 CUSTOM 类型）
     */
    int suffixLength() default 4;
}
