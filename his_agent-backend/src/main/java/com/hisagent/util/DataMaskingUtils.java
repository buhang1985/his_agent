package com.hisagent.util;

import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 */
public class DataMaskingUtils {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})\\d{8}(\\w{4})");
    private static final Pattern NAME_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{1})[\\u4e00-\\u9fa5]+([\\u4e00-\\u9fa5]{0,1})$");

    private DataMaskingUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 脱敏手机号
     * 13812345678 -> 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return PHONE_PATTERN.matcher(phone).replaceAll("$1****$2");
    }

    /**
     * 脱敏身份证号
     * 110101199001011234 -> 110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || (idCard.length() != 18 && idCard.length() != 15)) {
            return idCard;
        }
        if (idCard.length() == 18) {
            return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1********$2");
        }
        // 15 位身份证
        return idCard.replaceAll("(\\d{6})\\d{5}(\\w{4})", "$1*****$2");
    }

    /**
     * 脱敏姓名
     * 张三 -> 张*
     * 张三丰 -> 张*丰
     * 欧阳建华 -> 欧阳*华
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        // 处理复姓
        if (name.startsWith("欧阳") || name.startsWith("司马") || name.startsWith("上官") || 
            name.startsWith("诸葛") || name.startsWith("东方") || name.startsWith("西门")) {
            return name.substring(0, 2) + "*" + name.charAt(name.length() - 1);
        }
        return name.charAt(0) + "*" + name.charAt(name.length() - 1);
    }

    /**
     * 脱敏邮箱
     * test@example.com -> t**t@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        
        if (username.length() <= 2) {
            return username.charAt(0) + "*" + "@" + domain;
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1) + "@" + domain;
    }

    /**
     * 脱敏地址
     * 北京市海淀区中关村大街 1 号 -> 北京市海淀区****
     */
    public static String maskAddress(String address) {
        if (address == null || address.isEmpty()) {
            return address;
        }
        if (address.length() <= 6) {
            return address.substring(0, Math.min(2, address.length())) + "***";
        }
        return address.substring(0, 6) + "****";
    }

    /**
     * 部分脱敏（保留前后各 N 位）
     */
    public static String maskPartial(String value, int prefixLength, int suffixLength) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() <= prefixLength + suffixLength) {
            return "*".repeat(value.length());
        }
        return value.substring(0, prefixLength) + 
               "*".repeat(value.length() - prefixLength - suffixLength) + 
               value.substring(value.length() - suffixLength);
    }
}
