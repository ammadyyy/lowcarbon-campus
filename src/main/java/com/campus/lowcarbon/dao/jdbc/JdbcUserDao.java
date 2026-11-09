package com.campus.lowcarbon.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.campus.lowcarbon.common.DaoException;
import com.campus.lowcarbon.common.DuplicateException;
import com.campus.lowcarbon.dao.BaseJdbcDao;
import com.campus.lowcarbon.dao.ConnectionProvider;
import com.campus.lowcarbon.dao.SqlErrors;
import com.campus.lowcarbon.dao.UserDao;
import com.campus.lowcarbon.domain.Role;
import com.campus.lowcarbon.domain.User;

/** 基于 JDBC + SQLite 的用户 DAO 实现。 */
public class JdbcUserDao extends BaseJdbcDao implements UserDao {

    private static final String COLUMNS =
            "id, username, password_hash, role, credit_score, contact, banned, created_at";

    private static final String SQL_INSERT =
            "INSERT INTO users (username, password_hash, role, credit_score, contact, banned, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE users SET username = ?, password_hash = ?, role = ?, credit_score = ?, "
                    + "contact = ?, banned = ? WHERE id = ?";

    public JdbcUserDao(ConnectionProvider provider) {
        super(provider);
    }

    @Override
    public User insert(User user) {
        try {
            return withConnection(connection -> {
                try (PreparedStatement statement =
                             connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getPasswordHash());
                    statement.setString(3, user.getRole().code());
                    statement.setInt(4, user.getCreditScore());
                    statement.setString(5, user.getContact());
                    statement.setInt(6, user.isBanned() ? 1 : 0);
                    statement.setString(7, toDbTime(user.getCreatedAt()));
                    statement.executeUpdate();
                    long id = generatedKey(statement);
                    return user.toBuilder().id(id).build();
                }
            });
        } catch (DaoException ex) {
            if (SqlErrors.isUniqueViolation(ex)) {
                throw new DuplicateException("用户名已被占用：" + user.getUsername());
            }
            throw ex;
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.<User>empty();
                }
            }
        });
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE username = ?")) {
                statement.setString(1, username);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.<User>empty();
                }
            }
        });
    }

    @Override
    public List<User> findAll() {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM users ORDER BY id ASC");
                 ResultSet rows = statement.executeQuery()) {
                List<User> users = new ArrayList<User>();
                while (rows.next()) {
                    users.add(map(rows));
                }
                return users;
            }
        });
    }

    @Override
    public boolean update(User user) {
        if (!user.isPersisted()) {
            throw new DaoException("更新用户失败：缺少主键");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {
                statement.setString(1, user.getUsername());
                statement.setString(2, user.getPasswordHash());
                statement.setString(3, user.getRole().code());
                statement.setInt(4, user.getCreditScore());
                statement.setString(5, user.getContact());
                statement.setInt(6, user.isBanned() ? 1 : 0);
                statement.setLong(7, user.getId());
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean deleteById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM users WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public long count() {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        });
    }

    private static User map(ResultSet rows) throws SQLException {
        return User.builder()
                .id(rows.getLong("id"))
                .username(rows.getString("username"))
                .passwordHash(rows.getString("password_hash"))
                .role(Role.fromCode(rows.getString("role")))
                .creditScore(rows.getInt("credit_score"))
                .contact(rows.getString("contact"))
                .banned(rows.getInt("banned") == 1)
                .createdAt(fromDbTime(rows.getString("created_at")))
                .build();
    }
}
