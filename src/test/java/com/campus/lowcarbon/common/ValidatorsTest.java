package com.campus.lowcarbon.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 输入校验工具单元测试（等价类 + 边界值）。 */
@DisplayName("Validators 输入校验")
class ValidatorsTest {

    @Test
    @DisplayName("必填文本：去空白、空值、超长")
    void requireTextHandlesBoundaries() {
        assertEquals("教材", Validators.requireText("  教材  ", "标题", 1, 10));
        assertEquals("a", Validators.requireText("a", "标题", 1, 1));
        assertThrows(ValidationException.class, () -> Validators.requireText("   ", "标题", 1, 10));
        assertThrows(ValidationException.class, () -> Validators.requireText(null, "标题", 1, 10));
        assertThrows(ValidationException.class, () -> Validators.requireText("12345678901", "标题", 1, 10));
    }

    @Test
    @DisplayName("选填文本：null 转空串，超长报错")
    void optionalTextBehaviour() {
        assertEquals("", Validators.optionalText(null, "联系方式", 5));
        assertEquals("QQ", Validators.optionalText(" QQ ", "联系方式", 5));
        assertThrows(ValidationException.class, () -> Validators.optionalText("123456", "联系方式", 5));
    }

    @Test
    @DisplayName("用户名：合法格式通过，非法字符/长度被拒绝")
    void usernamePattern() {
        assertEquals("student_01", Validators.requireUsername(" student_01 "));
        assertThrows(ValidationException.class, () -> Validators.requireUsername("ab"));
        assertThrows(ValidationException.class, () -> Validators.requireUsername("张三"));
        assertThrows(ValidationException.class, () -> Validators.requireUsername("has space"));
        assertThrows(ValidationException.class, () -> Validators.requireUsername("user@campus"));
    }

    @Test
    @DisplayName("密码：长度与字母数字组合")
    void passwordPolicy() {
        assertEquals("abc12345", Validators.requirePassword("abc12345"));
        assertThrows(ValidationException.class, () -> Validators.requirePassword("abc1234"));
        assertThrows(ValidationException.class, () -> Validators.requirePassword("abcdefgh"));
        assertThrows(ValidationException.class, () -> Validators.requirePassword("12345678"));
        assertThrows(ValidationException.class, () -> Validators.requirePassword("abc 12345"));
    }

    @Test
    @DisplayName("价格：边界 0、上限 99999.99、两位小数")
    void priceValidation() {
        assertEquals(0, Validators.requirePrice(new BigDecimal("0")).compareTo(BigDecimal.ZERO));
        assertEquals(new BigDecimal("99999.99"), Validators.requirePrice(new BigDecimal("99999.99")));
        assertThrows(ValidationException.class, () -> Validators.requirePrice(new BigDecimal("-0.01")));
        assertThrows(ValidationException.class, () -> Validators.requirePrice(new BigDecimal("100000.00")));
        assertThrows(ValidationException.class, () -> Validators.requirePrice(new BigDecimal("1.234")));
        assertThrows(ValidationException.class, () -> Validators.requirePrice(null));
    }

    @Test
    @DisplayName("价格文本解析：非数字给出可读提示")
    void parsePriceFromText() {
        assertEquals(new BigDecimal("12.50"), Validators.parsePrice("12.50"));
        assertThrows(ValidationException.class, () -> Validators.parsePrice("十二元"));
        assertThrows(ValidationException.class, () -> Validators.parsePrice(""));
    }

    @Test
    @DisplayName("标签：混合分隔符、去重、最多 5 个、单标签 12 字")
    void tagNormalization() {
        assertEquals("教材,计算机", Validators.normalizeTags("教材，计算机; 教材"));
        assertEquals("", Validators.normalizeTags(null));
        List<String> tags = Validators.splitTags("教材,计算机,九成新");
        assertEquals(Arrays.asList("教材", "计算机", "九成新"), tags);
        assertTrue(Validators.splitTags(null).isEmpty());
        assertThrows(ValidationException.class,
                () -> Validators.normalizeTags("a,b,c,d,e,f"));
        assertThrows(ValidationException.class,
                () -> Validators.normalizeTags("这是一个超过十二个字符长度的标签"));
    }

    @Test
    @DisplayName("主键与范围校验")
    void idAndRangeValidation() {
        assertEquals(5L, Validators.requireId(5L, "物品编号"));
        assertThrows(ValidationException.class, () -> Validators.requireId(0, "物品编号"));
        assertEquals(3, Validators.requireRange(3, "页码", 1, 10));
        assertThrows(ValidationException.class, () -> Validators.requireRange(0, "页码", 1, 10));
    }
}
