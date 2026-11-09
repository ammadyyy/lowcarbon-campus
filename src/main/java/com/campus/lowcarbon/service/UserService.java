package com.campus.lowcarbon.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campus.lowcarbon.common.AuthException;
import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.Passwords;
import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.common.Validators;
import com.campus.lowcarbon.dao.UserDao;
import com.campus.lowcarbon.domain.Role;
import com.campus.lowcarbon.domain.User;

/**
 * 用户业务服务：注册、登录、口令修改、信用分与封禁管理。
 * <p>
 * 该层只依赖 {@link UserDao} 接口，不接触 JDBC / SQL，便于用内存假实现做单元测试。
 * </p>
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** 联系方式最大长度。 */
    private static final int CONTACT_MAX = 50;

    private final UserDao userDao;
    private final Passwords passwords;

    public UserService(UserDao userDao) {
        this(userDao, new Passwords());
    }

    public UserService(UserDao userDao, Passwords passwords) {
        if (userDao == null || passwords == null) {
            throw new IllegalArgumentException("UserService 依赖不能为空");
        }
        this.userDao = userDao;
        this.passwords = passwords;
    }

    /**
     * 学生注册。
     *
     * @return 已入库的用户（含主键）
     */
    public User register(String username, String rawPassword, String contact) {
        return doRegister(username, rawPassword, contact, Role.STUDENT);
    }

    /** 管理员注册（初始化管理员或由管理员后台创建）。 */
    public User registerAdmin(String username, String rawPassword, String contact) {
        return doRegister(username, rawPassword, contact, Role.ADMIN);
    }

    private User doRegister(String username, String rawPassword, String contact, Role role) {
        String safeName = Validators.requireUsername(username);
        String safePassword = Validators.requirePassword(rawPassword);
        String safeContact = Validators.optionalText(contact, "联系方式", CONTACT_MAX);

        if (userDao.findByUsername(safeName).isPresent()) {
            throw new DuplicateException("用户名已被占用：" + safeName);
        }

        User user = User.builder()
                .username(safeName)
                .passwordHash(passwords.hash(safePassword).encoded())
                .role(role)
                .creditScore(User.INITIAL_CREDIT)
                .contact(safeContact)
                .build();
        User saved = userDao.insert(user);
        log.info("注册成功：id={}, username={}, role={}", saved.getId(), saved.getUsername(), saved.getRole());
        return saved;
    }

    /**
     * 登录校验。
     * <p>用户名不存在与口令错误返回同一提示，避免账号枚举。</p>
     */
    public User login(String username, String rawPassword) {
        if (username == null || username.trim().isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new AuthException("用户名或密码错误");
        }
        Optional<User> found = userDao.findByUsername(username.trim());
        if (!found.isPresent()) {
            throw new AuthException("用户名或密码错误");
        }
        User user = found.get();
        if (!passwords.matches(rawPassword, user.getPasswordHash())) {
            throw new AuthException("用户名或密码错误");
        }
        user.ensureActive();
        log.info("登录成功：id={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    /** 修改口令：需要校验原口令，且新口令不得与原口令相同。 */
    public User changePassword(long userId, String oldPassword, String newPassword) {
        User user = requireUser(userId).ensureActive();
        if (!passwords.matches(oldPassword, user.getPasswordHash())) {
            throw new AuthException("原密码不正确");
        }
        String safeNew = Validators.requirePassword(newPassword);
        if (passwords.matches(safeNew, user.getPasswordHash())) {
            throw new ValidationException("新密码不能与原密码相同");
        }
        User updated = user.withPasswordHash(passwords.hash(safeNew).encoded());
        if (!userDao.update(updated)) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        log.info("修改密码成功：id={}", userId);
        return updated;
    }

    /** 更新联系方式。 */
    public User updateContact(long userId, String contact) {
        User user = requireUser(userId).ensureActive();
        String safeContact = Validators.optionalText(contact, "联系方式", CONTACT_MAX);
        User updated = user.withContact(safeContact);
        if (!userDao.update(updated)) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        return updated;
    }

    /** 管理员封禁 / 解封账号。 */
    public User setBanned(long userId, boolean banned) {
        User user = requireUser(userId);
        if (user.isAdmin() && banned) {
            throw new ValidationException("不能封禁管理员账号");
        }
        User updated = user.withBanned(banned);
        if (!userDao.update(updated)) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        log.info("{}账号：id={}", banned ? "封禁" : "解封", userId);
        return updated;
    }

    /** 调整信用分（评价、违约等场景调用），结果自动裁剪到 0-100。 */
    public User adjustCredit(long userId, int delta) {
        User user = requireUser(userId);
        User updated = user.adjustCredit(delta);
        if (!userDao.update(updated)) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        log.info("信用分调整：id={}, {} -> {}", userId, user.getCreditScore(), updated.getCreditScore());
        return updated;
    }

    /** 按主键查询，不存在时抛出业务异常。 */
    public User requireUser(long userId) {
        Validators.requireId(userId, "用户编号");
        Optional<User> found = userDao.findById(userId);
        if (!found.isPresent()) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        return found.get();
    }

    /** 查询用户，不存在返回空。 */
    public Optional<User> findById(long userId) {
        return userDao.findById(userId);
    }

    /** 按用户名查询，不存在返回空。 */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return userDao.findByUsername(username.trim());
    }

    /** 后台用户列表。 */
    public List<User> listAll() {
        return userDao.findAll();
    }

    /** 用户总数（统计用）。 */
    public long count() {
        return userDao.count();
    }
}
