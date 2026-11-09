package com.campus.lowcarbon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.campus.lowcarbon.common.DaoException;

/**
 * JDBC 数据访问模板类（模板方法模式）。
 * <p>
 * 子类只需实现"取连接之后做什么"，连接获取/关闭、异常包装、事务提交回滚
 * 等重复代码全部收敛到这里，这是消除"重复代码"坏味道的第一步重构。
 * </p>
 */
public abstract class BaseJdbcDao {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ConnectionProvider provider;

    protected BaseJdbcDao(ConnectionProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("ConnectionProvider 不能为空");
        }
        this.provider = provider;
    }

    protected ConnectionProvider provider() {
        return provider;
    }

    /** 模板方法：获取连接 → 执行 → 自动关闭连接 → 统一异常转换。 */
    protected <R> R withConnection(SqlFunction<R> work) {
        try (Connection connection = provider.getConnection()) {
            return work.apply(connection);
        } catch (SQLException ex) {
            throw new DaoException("数据库操作失败：" + ex.getMessage(), ex);
        }
    }

    /** 模板方法：在单个事务中执行，异常时回滚。 */
    protected <R> R inTransaction(SqlFunction<R> work) {
        try (Connection connection = provider.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                R result = work.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException ex) {
                rollbackQuietly(connection, ex);
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (SQLException ex) {
            throw new DaoException("事务执行失败：" + ex.getMessage(), ex);
        }
    }

    private static void rollbackQuietly(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    /** 读取自增主键。 */
    protected static long generatedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("插入记录后未获得自增主键");
    }

    /** 数据库中的时间以 ISO-8601 文本存储，便于直接阅读与导出。 */
    protected static String toDbTime(LocalDateTime time) {
        return time == null ? null : TIME_FORMAT.format(time);
    }

    /** 解析数据库中的时间文本。 */
    protected static LocalDateTime fromDbTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new DaoException("数据库中的时间格式非法：" + text, ex);
        }
    }

    /** LIKE 查询的参数转义，避免用户输入的 % 与 _ 变成通配符。 */
    protected static String likePattern(String keyword) {
        String escaped = keyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** 关闭语句时静默失败（用于 finally 中的清理）。 */
    protected static void closeQuietly(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 清理阶段的异常不应覆盖主流程异常
            }
        }
    }
}
