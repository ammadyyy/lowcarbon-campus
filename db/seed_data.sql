-- ============================================================
-- 低碳校园资源循环流转桌面系统 · 内置数据脚本（SQLite 3）
-- 迭代二产出：管理员账号 + 物品分类
-- 用法：sqlite3 data/campus.db < seed_data.sql
-- 说明：程序首次启动时 DataSeeder 也会自动创建同样的数据，
--       此脚本供"手工建库/演示建库"时使用，二者可重复执行。
-- 默认管理员：admin / admin123（PBKDF2-SHA256，120000 次迭代）
-- ============================================================

PRAGMA foreign_keys = ON;

-- 1. 管理员账号（口令散列由 Passwords.encode("admin123") 生成）
INSERT OR IGNORE INTO users (username, password_hash, role, credit_score, contact, banned, created_at)
VALUES ('admin',
        'pbkdf2$120000$nhE0r3mqSSfMJQxT+gMzdQ==$DbxRE/228X/7uNGLGIHjRQjO3TEN//AhEO2gGMzIdSM=',
        'ADMIN', 100, 'admin@campus.edu', 0, '2026-01-01T00:00:00');

-- 2. 物品分类（与 schema.sql 保持一致，便于单独执行本脚本）
INSERT OR IGNORE INTO categories (name, description, sort_order, enabled) VALUES
    ('教材书籍', '课本、考研资料、课外读物', 10, 1),
    ('数码配件', '耳机、键盘、数据线、充电器等', 20, 1),
    ('文体用品', '球拍、乐器、运动装备等', 30, 1),
    ('生活物资', '台灯、衣架、收纳、厨具等', 40, 1),
    ('其他闲置', '未归类闲置物品', 90, 1);
