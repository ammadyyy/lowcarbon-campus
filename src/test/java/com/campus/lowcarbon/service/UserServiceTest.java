package com.campus.lowcarbon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campus.lowcarbon.common.AuthException;
import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.Passwords;
import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.domain.Role;
import com.campus.lowcarbon.domain.User;
import com.campus.lowcarbon.support.InMemoryUserDao;

/** UserService 单元测试：使用内存 DAO，不依赖数据库。 */
@DisplayName("UserService 用户业务")
class UserServiceTest {

    private InMemoryUserDao userDao;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        userService = new UserService(userDao, new Passwords(1000));
    }

    @Test
    @DisplayName("注册成功：返回主键、角色为学生、口令以散列存储")
    void registerSavesHashedPassword() {
        User user = userService.register("student01", "abc12345", "13800000000");
        assertTrue(user.getId() > 0);
        assertEquals(Role.STUDENT, user.getRole());
        assertEquals(100, user.getCreditScore());
        assertNotEquals("abc12345", user.getPasswordHash());
        assertTrue(Passwords.isEncoded(user.getPasswordHash()));
        assertEquals(1, userService.count());
    }

    @Test
    @DisplayName("用户名重复与非法输入被拒绝")
    void registerRejectsInvalidInput() {
        userService.register("student01", "abc12345", "");
        assertThrows(DuplicateException.class, () -> userService.register("student01", "abc12345", ""));
        assertThrows(ValidationException.class, () -> userService.register("张三", "abc12345", ""));
        assertThrows(ValidationException.class, () -> userService.register("student02", "12345678", ""));
    }

    @Test
    @DisplayName("登录：口令正确通过，错误口令与不存在用户提示一致")
    void loginChecksPassword() {
        userService.register("student01", "abc12345", "");
        assertEquals("student01", userService.login("student01", "abc12345").getUsername());

        AuthException wrongPassword = assertThrows(AuthException.class,
                () -> userService.login("student01", "abc12346"));
        AuthException unknownUser = assertThrows(AuthException.class,
                () -> userService.login("nobody", "abc12345"));
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
        assertThrows(AuthException.class, () -> userService.login("", ""));
    }

    @Test
    @DisplayName("被封禁账号不能登录，解封后可登录")
    void bannedUserCannotLogin() {
        User user = userService.register("student01", "abc12345", "");
        userService.setBanned(user.getId(), true);
        assertThrows(AuthException.class, () -> userService.login("student01", "abc12345"));

        userService.setBanned(user.getId(), false);
        assertEquals("student01", userService.login("student01", "abc12345").getUsername());
    }

    @Test
    @DisplayName("修改密码：原密码错误被拒绝，新密码可用于登录")
    void changePasswordFlow() {
        User user = userService.register("student01", "abc12345", "");
        assertThrows(AuthException.class,
                () -> userService.changePassword(user.getId(), "wrong1234", "newpass123"));

        userService.changePassword(user.getId(), "abc12345", "newpass123");
        assertThrows(AuthException.class, () -> userService.login("student01", "abc12345"));
        assertTrue(userService.login("student01", "newpass123").getId() > 0);
    }

    @Test
    @DisplayName("新密码不能与原密码相同")
    void changePasswordRejectsSamePassword() {
        User user = userService.register("student01", "abc12345", "");
        assertThrows(ValidationException.class,
                () -> userService.changePassword(user.getId(), "abc12345", "abc12345"));
    }

    @Test
    @DisplayName("信用分调整被裁剪，联系方式可更新")
    void creditAndContact() {
        User user = userService.register("student01", "abc12345", "");
        assertEquals(100, userService.adjustCredit(user.getId(), 20).getCreditScore());
        assertEquals(80, userService.adjustCredit(user.getId(), -20).getCreditScore());
        assertEquals(0, userService.adjustCredit(user.getId(), -500).getCreditScore());

        User updated = userService.updateContact(user.getId(), " new@campus.edu ");
        assertEquals("new@campus.edu", updated.getContact());
    }

    @Test
    @DisplayName("不能封禁管理员；管理员注册角色正确")
    void adminRules() {
        User admin = userService.registerAdmin("admin01", "admin1234", "");
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
        assertThrows(ValidationException.class, () -> userService.setBanned(admin.getId(), true));
    }

    @Test
    @DisplayName("查询不存在用户抛出 NotFoundException")
    void requireUserThrowsWhenMissing() {
        assertThrows(NotFoundException.class, () -> userService.requireUser(999L));
        assertFalse(userService.findById(999L).isPresent());
        assertFalse(userService.findByUsername("nobody").isPresent());
    }
}
