package com.campus.lowcarbon.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campus.lowcarbon.common.AuthException;
import com.campus.lowcarbon.common.ValidationException;

/** User 实体单元测试：构造校验、信用分、封禁。 */
@DisplayName("User 实体")
class UserDomainTest {

    private User.Builder validBuilder() {
        return User.builder()
                .username("student01")
                .passwordHash("pbkdf2$1000$c2FsdA==$aGFzaA==")
                .role(Role.STUDENT)
                .creditScore(User.INITIAL_CREDIT)
                .contact("13800000000");
    }

    @Test
    @DisplayName("合法用户可创建，默认信用分 100")
    void buildValidUser() {
        User user = validBuilder().build();
        assertEquals("student01", user.getUsername());
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals(100, user.getCreditScore());
        assertFalse(user.isBanned());
        assertFalse(user.isPersisted());
        assertFalse(user.isAdmin());
    }

    @Test
    @DisplayName("用户名非法、散列为空、信用分越界均被拒绝")
    void rejectInvalidUser() {
        assertThrows(ValidationException.class, () -> validBuilder().username("张三").build());
        assertThrows(ValidationException.class, () -> validBuilder().username("ab").build());
        assertThrows(ValidationException.class, () -> validBuilder().passwordHash(" ").build());
        assertThrows(ValidationException.class, () -> validBuilder().creditScore(101).build());
        assertThrows(ValidationException.class, () -> validBuilder().creditScore(-1).build());
        assertThrows(ValidationException.class, () -> validBuilder().role(null).build());
    }

    @Test
    @DisplayName("信用分增减被裁剪到 0-100")
    void creditIsClamped() {
        User user = validBuilder().creditScore(95).build();
        assertEquals(100, user.adjustCredit(20).getCreditScore());
        assertEquals(90, user.adjustCredit(-5).getCreditScore());
        assertEquals(0, validBuilder().creditScore(3).build().adjustCredit(-10).getCreditScore());
    }

    @Test
    @DisplayName("封禁账号不能通过 ensureActive")
    void bannedUserIsRejected() {
        User banned = validBuilder().banned(true).build();
        assertThrows(AuthException.class, banned::ensureActive);
        assertTrue(validBuilder().build().ensureActive().getId() == 0L);
    }

    @Test
    @DisplayName("with 系列方法返回新对象，不修改原对象（不可变实体）")
    void withMethodsAreImmutable() {
        User user = validBuilder().build();
        User updated = user.withContact("new@campus.edu").withBanned(true);
        assertEquals("13800000000", user.getContact());
        assertFalse(user.isBanned());
        assertEquals("new@campus.edu", updated.getContact());
        assertTrue(updated.isBanned());
    }

    @Test
    @DisplayName("按用户名判断相等性")
    void equalityByUsername() {
        User left = validBuilder().id(1).build();
        User right = validBuilder().id(2).build();
        User other = validBuilder().username("student02").build();
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertNotEquals(left, other);
    }
}
