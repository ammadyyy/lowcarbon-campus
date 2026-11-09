package com.campus.lowcarbon.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 表结构初始化与约束测试（集成测试：真实 SQLite 文件）。 */
@DisplayName("Database 建表与约束")
class DatabaseSchemaTest {

    @Test
    @DisplayName("initialize 建出全部业务表并写入 5 条分类")
    void initializeCreatesSchema(@TempDir Path tempDir) throws Exception {
        Database database = new Database(sqliteUrl(tempDir));
        database.initialize();
        assertTrue(database.isInitialized());

        Set<String> tables = new HashSet<String>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name")) {
            while (rows.next()) {
                tables.add(rows.getString(1));
            }
        }
        assertTrue(tables.contains("users"), "缺少 users 表：" + tables);
        assertTrue(tables.contains("categories"));
        assertTrue(tables.contains("items"));
        assertTrue(tables.contains("favorites"));
        assertTrue(tables.contains("exchange_records"));
        assertTrue(tables.contains("reviews"));
        assertTrue(tables.contains("operation_logs"));

        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM categories")) {
            assertEquals(5, rows.next() ? rows.getInt(1) : -1);
        }
    }

    @Test
    @DisplayName("initialize 幂等：重复执行不报错也不重复插入")
    void initializeIsIdempotent(@TempDir Path tempDir) throws Exception {
        Database database = new Database(sqliteUrl(tempDir));
        database.initialize();
        database.initialize();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM categories")) {
            assertEquals(5, rows.next() ? rows.getInt(1) : -1);
        }
    }

    @Test
    @DisplayName("外键开启：删除用户级联删除其物品")
    void foreignKeyCascade(@TempDir Path tempDir) throws Exception {
        Database database = new Database(sqliteUrl(tempDir));
        database.initialize();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO users (username, password_hash, role, credit_score, contact, banned, created_at) "
                    + "VALUES ('cascade01', 'pbkdf2$x$y$z', 'STUDENT', 100, '', 0, '2026-01-01T00:00:00')");
            statement.executeUpdate("INSERT INTO items (owner_id, category_id, title, description, price, trade_type, "
                    + "status, condition_level, tags, favorite_count, created_at, updated_at) "
                    + "VALUES (1, 1, '教材', '', '10.00', 'SELL', 'ON_SALE', 'GOOD', '', 0, "
                    + "'2026-01-01T00:00:00', '2026-01-01T00:00:00')");
            statement.executeUpdate("DELETE FROM users WHERE username = 'cascade01'");
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM items")) {
                assertEquals(0, rows.next() ? rows.getInt(1) : -1);
            }
        }
    }

    @Test
    @DisplayName("数据库文件路径可定位且目录自动创建")
    void databaseFileAndDirectory(@TempDir Path tempDir) {
        Path nested = tempDir.resolve("nested").resolve("data");
        Database database = new Database("jdbc:sqlite:" + nested.resolve("app.db").toString().replace('\\', '/'));
        database.initialize();
        assertNotNull(database.getFilePath());
        assertTrue(database.getFilePath().toFile().isFile());
        assertEquals("jdbc:sqlite:", database.getUrl().substring(0, 12));
    }

    static String sqliteUrl(Path directory) {
        return "jdbc:sqlite:" + directory.resolve("schema-test.db").toAbsolutePath().toString().replace('\\', '/');
    }
}
