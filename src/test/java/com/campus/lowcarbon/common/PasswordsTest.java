package com.campus.lowcarbon.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 口令散列模块单元测试。 */
@DisplayName("Passwords 口令散列")
class PasswordsTest {

    private final Passwords passwords = new Passwords(1000);

    @Test
    @DisplayName("散列结果是带迭代次数与盐的编码串，且不含明文")
    void hashProducesEncodedString() {
        String encoded = passwords.hash("secret123").encoded();
        assertTrue(encoded.startsWith("pbkdf2$1000$"), "编码串应包含算法与迭代次数：" + encoded);
        assertTrue(Passwords.isEncoded(encoded));
        assertFalse(encoded.contains("secret123"));
    }

    @Test
    @DisplayName("相同口令两次散列结果不同（随机盐）")
    void samePasswordProducesDifferentHashes() {
        String first = passwords.hash("secret123").encoded();
        String second = passwords.hash("secret123").encoded();
        assertNotEquals(first, second);
        assertTrue(passwords.matches("secret123", first));
        assertTrue(passwords.matches("secret123", second));
    }

    @Test
    @DisplayName("正确口令校验通过，错误口令校验失败")
    void matchesOnlyCorrectPassword() {
        String encoded = passwords.hash("campus2026").encoded();
        assertTrue(passwords.matches("campus2026", encoded));
        assertFalse(passwords.matches("campus2027", encoded));
        assertFalse(passwords.matches("Campus2026", encoded));
    }

    @Test
    @DisplayName("非法或空编码串一律返回 false，不抛异常")
    void invalidEncodedStringReturnsFalse() {
        assertFalse(passwords.matches("secret123", null));
        assertFalse(passwords.matches("secret123", ""));
        assertFalse(passwords.matches("secret123", "md5$abc"));
        assertFalse(passwords.matches("secret123", "pbkdf2$notanumber$c2FsdA==$aGFzaA=="));
        assertFalse(passwords.matches("secret123", "pbkdf2$1000$***$***"));
        assertFalse(Passwords.isEncoded(null));
        assertFalse(Passwords.isEncoded("plain-text"));
    }

    @Test
    @DisplayName("静态 encode 与实例 hash 互通")
    void staticEncodeRoundTrip() {
        String encoded = Passwords.encode("abc12345");
        assertTrue(Passwords.isEncoded(encoded));
        assertTrue(new Passwords().matches("abc12345", encoded));
    }

    @Test
    @DisplayName("空口令与非法迭代次数被拒绝")
    void rejectsInvalidArguments() {
        assertThrows(ValidationException.class, () -> passwords.hash(""));
        assertThrows(ValidationException.class, () -> passwords.hash(null));
        assertThrows(ValidationException.class, () -> new Passwords(0));
    }

    @Test
    @DisplayName("随机口令长度正确且满足强度要求")
    void randomPasswordMeetsPolicy() {
        String generated = Passwords.randomPassword(12);
        assertTrue(generated.length() == 12);
        assertTrue(Validators.requirePassword(generated).length() == 12);
    }
}
