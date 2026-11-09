package com.campus.lowcarbon.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.campus.lowcarbon.common.DaoException;
import com.campus.lowcarbon.config.AppConfig;

/**
 * SQLite 数据库门面（单例模式）。
 * <p>
 * 负责：连接创建、外键开关、表结构初始化。应用全局只保留一个实例；
 * 单元测试通过 {@link #Database(String)} 构造隔离的临时库，互不干扰。
 * </p>
 */
public final class Database implements ConnectionProvider {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());
    private static final String SQLITE_PREFIX = "jdbc:sqlite:";

    private static volatile Database instance;

    private final String url;
    private final String schemaResource;
    private volatile boolean initialized;

    public Database(String url) {
        this(url, "/db/schema.sql");
    }

    public Database(String url, String schemaResource) {
        if (url == null || !url.startsWith(SQLITE_PREFIX)) {
            throw new IllegalArgumentException("仅支持 SQLite 数据库，当前 URL：" + url);
        }
        this.url = url;
        this.schemaResource = schemaResource;
        // 文件型数据库先建好父目录，否则 SQLite 会报 "path does not exist"
        ensureParentDirectory(url);
    }

    /** 获取全局单例（双重检查锁，保证线程安全且只初始化一次）。 */
    public static Database getInstance() {
        Database local = instance;
        if (local == null) {
            synchronized (Database.class) {
                local = instance;
                if (local == null) {
                    local = createDefault();
                    instance = local;
                }
            }
        }
        return local;
    }

    private static Database createDefault() {
        String url = AppConfig.get("db.url", SQLITE_PREFIX + "data/campus.db");
        ensureParentDirectory(url);
        return new Database(url);
    }

    private static void ensureParentDirectory(String url) {
        String path = url.substring(SQLITE_PREFIX.length());
        if (path.isEmpty() || path.startsWith(":memory:") || path.startsWith("file:")) {
            return;
        }
        Path file = Paths.get(path).toAbsolutePath();
        Path parent = file.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException ex) {
            throw new DaoException("无法创建数据库目录：" + parent, ex);
        }
    }

    /** 数据库连接地址。 */
    public String getUrl() {
        return url;
    }

    /** 数据库文件路径（内存库返回 null）。 */
    public Path getFilePath() {
        String path = url.substring(SQLITE_PREFIX.length());
        if (path.isEmpty() || path.startsWith(":memory:") || path.startsWith("file:")) {
            return null;
        }
        return Paths.get(path).toAbsolutePath();
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            // SQLite 的外键约束默认关闭，必须逐连接开启，否则级联删除会静默失效
            statement.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException ex) {
            connection.close();
            throw ex;
        }
        return connection;
    }

    /** 幂等初始化：执行 schema.sql 中的建表语句。 */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        List<String> statements = loadSchemaStatements();
        try (Connection connection = getConnection()) {
            for (String sql : statements) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
                }
            }
            initialized = true;
            LOG.info("数据库初始化完成：" + url + "（" + statements.size() + " 条语句）");
        } catch (SQLException ex) {
            throw new DaoException("数据库初始化失败：" + ex.getMessage(), ex);
        }
    }

    /** 是否已完成初始化。 */
    public boolean isInitialized() {
        return initialized;
    }

    private List<String> loadSchemaStatements() {
        String script = readResource(schemaResource);
        List<String> statements = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        for (String rawLine : script.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            buffer.append(rawLine).append('\n');
            if (line.endsWith(";")) {
                String sql = buffer.toString().trim();
                sql = sql.substring(0, sql.length() - 1).trim();
                if (!sql.isEmpty()) {
                    statements.add(sql);
                }
                buffer.setLength(0);
            }
        }
        String remainder = buffer.toString().trim();
        if (!remainder.isEmpty()) {
            statements.add(remainder);
        }
        if (statements.isEmpty()) {
            throw new DaoException("建表脚本为空：" + schemaResource);
        }
        return statements;
    }

    private static String readResource(String resource) {
        try (InputStream input = Database.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new DaoException("找不到数据库脚本：" + resource);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new DaoException("读取数据库脚本失败：" + resource, ex);
        }
    }
}
