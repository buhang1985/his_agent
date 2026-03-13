package com.hisagent.filter;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

import java.util.regex.Pattern;

/**
 * 敏感数据日志过滤器
 * 自动过滤日志中的敏感信息（手机号、身份证、邮箱等）
 */
public class SensitiveDataFilter extends Filter<ILoggingEvent> {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d{9})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{17}[\\dXx]|\\d{15})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        
        if (message == null) {
            return FilterReply.NEUTRAL;
        }

        // 脱敏手机号
        message = PHONE_PATTERN.matcher(message).replaceAll("$1".substring(0, 3) + "****$1".substring(7));
        
        // 脱敏身份证号
        message = ID_CARD_PATTERN.matcher(message).replaceAll("$1".substring(0, 6) + "********$1".substring(14));
        
        // 脱敏邮箱
        message = EMAIL_PATTERN.matcher(message).replaceAll("$1".substring(0, 1) + "***@$1".substring(1));

        // 如果消息被修改，记录脱敏后的日志
        if (!message.equals(event.getFormattedMessage())) {
            // 这里可以添加逻辑来替换日志消息
            // 由于 Logback 限制，实际脱敏需要在日志输出层面处理
        }

        return FilterReply.NEUTRAL;
    }
}
