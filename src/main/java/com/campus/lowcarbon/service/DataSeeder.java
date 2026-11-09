package com.campus.lowcarbon.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campus.lowcarbon.domain.User;

/**
 * 初始化数据填充器：保证系统首次启动即可登录、可选择分类。
 * <p>
 * 管理员口令在这里通过 {@link Passwords} 真实散列后入库，
 * 数据库中不出现任何固定假散列，避免"看似加密实则不可登录"的问题。
 * </p>
 */
public final class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** 默认管理员账号。 */
    public static final String ADMIN_USERNAME = "admin";

    /** 默认管理员口令（首次登录后应立即修改）。 */
    public static final String ADMIN_PASSWORD = "admin123";

    private final UserService userService;
    private final CategoryService categoryService;

    public DataSeeder(UserService userService, CategoryService categoryService) {
        if (userService == null || categoryService == null) {
            throw new IllegalArgumentException("DataSeeder 依赖不能为空");
        }
        this.userService = userService;
        this.categoryService = categoryService;
    }

    /** 幂等初始化：缺管理员就建，缺分类就补。 */
    public void bootstrap() {
        ensureAdmin();
        ensureCategories();
    }

    /**
     * 确保存在默认管理员。
     *
     * @return 管理员账号
     */
    public User ensureAdmin() {
        Optional<User> existing = userService.findByUsername(ADMIN_USERNAME);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.isAdmin()) {
                log.warn("账号 {} 已存在但不是管理员，请手工检查数据", ADMIN_USERNAME);
            }
            return user;
        }
        User admin = userService.registerAdmin(ADMIN_USERNAME, ADMIN_PASSWORD, "admin@campus.edu");
        log.info("已创建默认管理员：{} / {}（请在首次登录后修改密码）", ADMIN_USERNAME, ADMIN_PASSWORD);
        return admin;
    }

    /** 分类表为空时写入默认分类（schema.sql 已内置时不会重复插入）。 */
    public void ensureCategories() {
        if (categoryService.count() > 0) {
            return;
        }
        categoryService.create("教材书籍", "课本、考研资料、课外读物", 10);
        categoryService.create("数码配件", "数据线、耳机、键盘等", 20);
        categoryService.create("文体用品", "球类、乐器、画材", 30);
        categoryService.create("生活物资", "小家电、收纳、日用", 40);
        categoryService.create("其他闲置", "未归类的闲置物品", 50);
        log.info("已写入默认分类：{} 个", categoryService.count());
    }
}
