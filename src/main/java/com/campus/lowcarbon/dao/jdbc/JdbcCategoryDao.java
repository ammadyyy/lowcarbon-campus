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
import com.campus.lowcarbon.dao.CategoryDao;
import com.campus.lowcarbon.dao.ConnectionProvider;
import com.campus.lowcarbon.dao.SqlErrors;
import com.campus.lowcarbon.domain.Category;

/** 基于 JDBC + SQLite 的分类 DAO 实现。 */
public class JdbcCategoryDao extends BaseJdbcDao implements CategoryDao {

    private static final String COLUMNS = "id, name, description, sort_order, enabled";

    public JdbcCategoryDao(ConnectionProvider provider) {
        super(provider);
    }

    @Override
    public Category insert(Category category) {
        try {
            return withConnection(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO categories (name, description, sort_order, enabled) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, category.getName());
                    statement.setString(2, category.getDescription());
                    statement.setInt(3, category.getSortOrder());
                    statement.setInt(4, category.isEnabled() ? 1 : 0);
                    statement.executeUpdate();
                    long id = generatedKey(statement);
                    return category.toBuilder().id(id).build();
                }
            });
        } catch (DaoException ex) {
            if (SqlErrors.isUniqueViolation(ex)) {
                throw new DuplicateException("分类名称已存在：" + category.getName());
            }
            throw ex;
        }
    }

    @Override
    public Optional<Category> findById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM categories WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.<Category>empty();
                }
            }
        });
    }

    @Override
    public Optional<Category> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM categories WHERE name = ?")) {
                statement.setString(1, name);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.<Category>empty();
                }
            }
        });
    }

    @Override
    public List<Category> findAll(boolean enabledOnly) {
        String sql = "SELECT " + COLUMNS + " FROM categories"
                + (enabledOnly ? " WHERE enabled = 1" : "")
                + " ORDER BY sort_order ASC, id ASC";
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rows = statement.executeQuery()) {
                List<Category> categories = new ArrayList<Category>();
                while (rows.next()) {
                    categories.add(map(rows));
                }
                return categories;
            }
        });
    }

    @Override
    public boolean update(Category category) {
        if (category.getId() <= 0) {
            throw new DaoException("更新分类失败：缺少主键");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE categories SET name = ?, description = ?, sort_order = ?, enabled = ? WHERE id = ?")) {
                statement.setString(1, category.getName());
                statement.setString(2, category.getDescription());
                statement.setInt(3, category.getSortOrder());
                statement.setInt(4, category.isEnabled() ? 1 : 0);
                statement.setLong(5, category.getId());
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean deleteById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public long count() {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM categories");
                 ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        });
    }

    private static Category map(ResultSet rows) throws SQLException {
        return Category.builder()
                .id(rows.getLong("id"))
                .name(rows.getString("name"))
                .description(rows.getString("description"))
                .sortOrder(rows.getInt("sort_order"))
                .enabled(rows.getInt("enabled") == 1)
                .build();
    }
}
