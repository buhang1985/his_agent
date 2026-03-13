package com.hisagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页响应基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;

    /**
     * 创建分页响应
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean first = page == 0;
        boolean last = page >= totalPages - 1;
        boolean empty = content == null || content.isEmpty();

        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            first,
            last,
            empty
        );
    }

    /**
     * 创建空分页响应
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return of(List.of(), page, size, 0);
    }
}
