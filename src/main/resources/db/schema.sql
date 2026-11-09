-- ============================================================
-- 低碳校园资源循环流转桌面系统 · 数据库初始化脚本（SQLite 3）
-- 迭代一产出：完整的表结构设计
-- 迭代二使用：users / categories / items / favorites
-- 迭代三、四使用：exchange_records / reviews / operation_logs
-- 用法：sqlite3 campus.db < schema.sql
-- ============================================================

PRAGMA foreign_keys = ON;

-- ------------------------------------------------------------
-- 1. 用户表：账号、角色、信用分、封禁状态
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT    NOT NULL UNIQUE,
    password_hash TEXT    NOT NULL,              -- PBKDF2 编码串 pbkdf2$迭代次数$盐$摘要
    role          TEXT    NOT NULL DEFAULT 'STUDENT'
                          CHECK (role IN ('STUDENT', 'ADMIN')),
    credit_score  INTEGER NOT NULL DEFAULT 100
                          CHECK (credit_score BETWEEN 0 AND 100),
    contact       TEXT,
    banned        INTEGER NOT NULL DEFAULT 0 CHECK (banned IN (0, 1)),
    created_at    TEXT    NOT NULL
);

-- ------------------------------------------------------------
-- 2. 物品分类表：管理员可维护
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    enabled     INTEGER NOT NULL DEFAULT 1 CHECK (enabled IN (0, 1))
);

-- ------------------------------------------------------------
-- 3. 闲置物品表：发布、检索、流转状态
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id        INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id     INTEGER NOT NULL REFERENCES categories (id),
    title           TEXT    NOT NULL,
    description     TEXT    NOT NULL DEFAULT '',
    price           NUMERIC NOT NULL DEFAULT 0 CHECK (price >= 0),
    trade_type      TEXT    NOT NULL
                            CHECK (trade_type IN ('SELL', 'EXCHANGE', 'DONATE')),
    status          TEXT    NOT NULL DEFAULT 'ON_SALE'
                            CHECK (status IN ('ON_SALE', 'LOCKED', 'OFF_SHELF', 'COMPLETED')),
    condition_level TEXT    NOT NULL DEFAULT 'GOOD'
                            CHECK (condition_level IN ('NEW', 'LIKE_NEW', 'GOOD', 'FAIR')),
    tags            TEXT    NOT NULL DEFAULT '',   -- 逗号分隔，迭代四可拆表
    favorite_count  INTEGER NOT NULL DEFAULT 0 CHECK (favorite_count >= 0),
    created_at      TEXT    NOT NULL,
    updated_at      TEXT    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_items_owner    ON items (owner_id);
CREATE INDEX IF NOT EXISTS idx_items_category ON items (category_id);
CREATE INDEX IF NOT EXISTS idx_items_status   ON items (status);
CREATE INDEX IF NOT EXISTS idx_items_title    ON items (title);

-- ------------------------------------------------------------
-- 4. 收藏表：用户收藏物品
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    item_id    INTEGER NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    created_at TEXT    NOT NULL,
    PRIMARY KEY (user_id, item_id)
);

-- ------------------------------------------------------------
-- 5. 流转记录表（迭代三接入）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS exchange_records (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id       INTEGER NOT NULL REFERENCES items (id) ON DELETE CASCADE,
    requester_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    owner_id      INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    message       TEXT,
    status        TEXT    NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'FINISHED', 'CANCELED')),
    created_at    TEXT    NOT NULL,
    finished_at   TEXT
);

CREATE INDEX IF NOT EXISTS idx_exchange_item ON exchange_records (item_id);
CREATE INDEX IF NOT EXISTS idx_exchange_user ON exchange_records (requester_id);

-- ------------------------------------------------------------
-- 6. 评价表（迭代三接入）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    record_id   INTEGER NOT NULL REFERENCES exchange_records (id) ON DELETE CASCADE,
    rater_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_id   INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score       INTEGER NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment     TEXT,
    created_at  TEXT    NOT NULL,
    UNIQUE (record_id, rater_id)
);

-- ------------------------------------------------------------
-- 7. 操作日志表（迭代四接入：日志记录关键操作）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS operation_logs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    action     TEXT    NOT NULL,
    detail     TEXT,
    created_at TEXT    NOT NULL
);

-- ============================================================
-- 内置初始数据：物品分类
-- ============================================================
INSERT OR IGNORE INTO categories (name, description, sort_order, enabled) VALUES
    ('教材书籍', '课本、考研资料、课外读物', 10, 1),
    ('数码配件', '耳机、键盘、数据线、充电器等', 20, 1),
    ('文体用品', '球拍、乐器、运动装备等', 30, 1),
    ('生活物资', '台灯、衣架、收纳、厨具等', 40, 1),
    ('其他闲置', '未归类闲置物品', 90, 1);
