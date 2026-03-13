package com.hisagent.dto;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PageResponse 单元测试
 */
class PageResponseTest {

    @Test
    void testPageResponseOf() {
        List<String> content = List.of("item1", "item2", "item3");
        PageResponse<String> response = PageResponse.of(content, 0, 10, 25);

        assertEquals(3, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(25, response.getTotalElements());
        assertEquals(3, response.getTotalPages());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());
        assertFalse(response.isEmpty());
    }

    @Test
    void testPageResponseLastPage() {
        List<String> content = List.of("item21", "item22");
        PageResponse<String> response = PageResponse.of(content, 2, 10, 22);

        assertEquals(2, response.getContent().size());
        assertEquals(2, response.getPage());
        assertEquals(3, response.getTotalPages());
        assertFalse(response.isFirst());
        assertTrue(response.isLast());
    }

    @Test
    void testPageResponseEmpty() {
        PageResponse<String> response = PageResponse.empty(0, 20);

        assertTrue(response.getContent().isEmpty());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
        assertTrue(response.isEmpty());
    }

    @Test
    void testPageResponseZeroTotalPages() {
        PageResponse<String> response = PageResponse.of(List.of(), 0, 10, 0);
        assertEquals(0, response.getTotalPages());
    }
}
