package com.campus.lowcarbon.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.common.Passwords;
import com.campus.lowcarbon.domain.Role;
import com.campus.lowcarbon.domain.User;
import com.campus.lowcarbon.support.TestContext;

/** JdbcUserDao 集成测试：真实 SQLite、唯一约束、增删改查。 */
@DisplayName("JdbcUserDao 用户持久化")
class JdbcUserDaoTest {

    private UserDao userDao;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        TestContext context = new TestContext(tempDir);
        userDao = context.daoFactory().userDao();
    }

    private User newUser(String username) {
        return User.builder()
                .username(username)
                .passwordHash(Passwords.encode("abc12345"))
                .role(Role.STUDENT)
                .creditScore(100)
                .contact("13800000000")
                .build();
    }

    @Test
    @DisplayName("插入返回自增主键，可按 id 与用户名查回")
    void insertAndFind() {
        User saved = userDao.insert(newUser("student01"));
        assertTrue(saved.getId() > 0);

        Optional<User> byId = userDao.findById(saved.getId());
        assertTrue(byId.isPresent());
        assertEquals("student01", byId.get().getUsername());
        assertEquals(Role.STUDENT, byId.get().getRole());
        assertEquals(100, byId.get().getCreditScore());

        assertTrue(userDao.findByUsername("student01").isPresent());
        assertFalse(userDao.findByUsername("student02").isPresent());
        assertFalse(userDao.findByUsername(null).isPresent());
    }

    @Test
    @DisplayName("用户名唯一约束被翻译为 DuplicateException")
    void duplicateUsernameRejected() {
        userDao.insert(newUser("student01"));
        assertThrows(DuplicateException.class, () -> userDao.insert(newUser("student01")));
        assertEquals(1, userDao.count());
    }

    @Test
    @DisplayName("更新与封禁状态持久化")
    void updatePersistsChanges() {
        User saved = userDao.insert(newUser("student01"));
        User banned = saved.withBanned(true).withContact("new@campus.edu");
        assertTrue(userDao.update(banned));

        User reloaded = userDao.findById(saved.getId()).get();
        assertTrue(reloaded.isBanned());
        assertEquals("new@campus.edu", reloaded.getContact());

        assertFalse(userDao.update(saved.toBuilder().id(999).build()));
    }

    @Test
    @DisplayName("查询全部按 id 升序，删除后计数减少")
    void findAllAndDelete() {
        userDao.insert(newUser("student01"));
        userDao.insert(newUser("student02"));
        List<User> all = userDao.findAll();
        assertEquals(2, all.size());
        assertTrue(all.get(0).getId() < all.get(1).getId());

        assertTrue(userDao.deleteById(all.get(0).getId()));
        assertEquals(1, userDao.count());
        assertFalse(userDao.deleteById(all.get(0).getId()));
    }
}
