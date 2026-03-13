package com.hisagent.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DataMaskingUtils 单元测试
 */
class DataMaskingUtilsTest {

    @Test
    void testMaskPhone() {
        assertEquals("138****5678", DataMaskingUtils.maskPhone("13812345678"));
        assertEquals("199****9999", DataMaskingUtils.maskPhone("19999999999"));
        assertNull(DataMaskingUtils.maskPhone(null));
        assertEquals("12345", DataMaskingUtils.maskPhone("12345"));
    }

    @Test
    void testMaskIdCard() {
        assertEquals("110101********1234", DataMaskingUtils.maskIdCard("110101199001011234"));
        assertEquals("440301********567X", DataMaskingUtils.maskIdCard("44030119950101567X"));
        assertNull(DataMaskingUtils.maskIdCard(null));
        assertEquals("12345", DataMaskingUtils.maskIdCard("12345"));
    }

    @Test
    void testMaskName() {
        assertEquals("张*", DataMaskingUtils.maskName("张三"));
        assertEquals("张*丰", DataMaskingUtils.maskName("张三丰"));
        assertEquals("欧阳*华", DataMaskingUtils.maskName("欧阳建华"));
        assertEquals("*", DataMaskingUtils.maskName("张"));
        assertEquals("欧阳*华", DataMaskingUtils.maskName("欧阳建华"));
        assertNull(DataMaskingUtils.maskName(null));
    }

    @Test
    void testMaskEmail() {
        assertEquals("t***t@example.com", DataMaskingUtils.maskEmail("test@example.com"));
        assertEquals("a*@gmail.com", DataMaskingUtils.maskEmail("ab@gmail.com"));
        assertEquals("a*@qq.com", DataMaskingUtils.maskEmail("a@qq.com"));
        assertNull(DataMaskingUtils.maskEmail(null));
    }

    @Test
    void testMaskAddress() {
        assertEquals("北京市海淀区****", DataMaskingUtils.maskAddress("北京市海淀区中关村大街 1 号"));
        assertEquals("广东省深圳市****", DataMaskingUtils.maskAddress("广东省深圳市南山区"));
        assertEquals("北京***", DataMaskingUtils.maskAddress("北京"));
        assertNull(DataMaskingUtils.maskAddress(null));
    }

    @Test
    void testMaskPartial() {
        assertEquals("abc***xyz", DataMaskingUtils.maskPartial("abc123xyz", 3, 3));
        assertEquals("ab**ef", DataMaskingUtils.maskPartial("abcdef", 2, 2));
        assertEquals("******", DataMaskingUtils.maskPartial("abcdef", 3, 3));
        assertEquals("****", DataMaskingUtils.maskPartial("abcd", 3, 3));
        assertNull(DataMaskingUtils.maskPartial(null, 3, 3));
    }
}
