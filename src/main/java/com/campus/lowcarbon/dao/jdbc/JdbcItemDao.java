package com.campus.lowcarbon.dao.jdbc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.campus.lowcarbon.common.DaoException;
import com.campus.lowcarbon.common.Validators;
import com.campus.lowcarbon.dao.BaseJdbcDao;
import com.campus.lowcarbon.dao.ConnectionProvider;
import com.campus.lowcarbon.dao.ItemDao;
import com.campus.lowcarbon.dao.ItemQuery;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemCondition;
import com.campus.lowcarbon.domain.ItemStatus;
import com.campus.lowcarbon.domain.TradeType;

/**
 * 基于 JDBC + SQLite 的物品 DAO 实现。
 * <p>
 * 检索使用"动态条件 + 参数占位符"，所有用户输入一律通过 PreparedStatement 绑定，
 * 从机制上杜绝 SQL 注入；排序子句由 {@link com.campus.lowcarbon.dao.ItemSortStrategy} 提供常量。
 * </p>
 */
public class JdbcItemDao extends BaseJdbcDao implements ItemDao {

    private static final String COLUMNS = "id, owner_id, category_id, title, description, price, "
            + "trade_type, status, condition_level, tags, favorite_count, created_at, updated_at";

    private static final String SQL_INSERT = "INSERT INTO items (owner_id, category_id, title, description, "
            + "price, trade_type, status, condition_level, tags, favorite_count, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public JdbcItemDao(ConnectionProvider provider) {
        super(provider);
    }

    @Override
    public Item insert(Item item) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, item.getOwnerId());
                statement.setLong(2, item.getCategoryId());
                statement.setString(3, item.getTitle());
                statement.setString(4, item.getDescription());
                statement.setString(5, item.getPrice().toPlainString());
                statement.setString(6, item.getTradeType().code());
                statement.setString(7, item.getStatus().code());
                statement.setString(8, item.getCondition().code());
                statement.setString(9, String.join(Validators.TAG_SEPARATOR, item.getTags()));
                statement.setInt(10, item.getFavoriteCount());
                statement.setString(11, toDbTime(item.getCreatedAt()));
                statement.setString(12, toDbTime(item.getUpdatedAt()));
                statement.executeUpdate();
                long id = generatedKey(statement);
                return item.toBuilder().id(id).build();
            }
        });
    }

    @Override
    public Optional<Item> findById(long id) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT " + COLUMNS + " FROM items WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.<Item>empty();
                }
            }
        });
    }

    @Override
    public List<Item> search(ItemQuery query) {
        List<Object> parameters = new ArrayList<Object>();
        String where = buildWhere(query, parameters);
        String sql = "SELECT " + COLUMNS + " FROM items" + where
                + " ORDER BY " + query.getSort().orderByClause()
                + " LIMIT ? OFFSET ?";
        parameters.add(query.getLimit());
        parameters.add(query.getOffset());
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Item> items = new ArrayList<Item>();
                    while (rows.next()) {
                        items.add(map(rows));
                    }
                    return items;
                }
            }
        });
    }

    @Override
    public int count(ItemQuery query) {
        List<Object> parameters = new ArrayList<Object>();
        String where = buildWhere(query, parameters);
        String sql = "SELECT COUNT(*) FROM items" + where;
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public boolean update(Item item) {
        if (item.getId() <= 0) {
            throw new DaoException("更新物品失败：缺少主键");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE items SET category_id = ?, title = ?, description = ?, price = ?, "
                            + "trade_type = ?, condition_level = ?, tags = ?, updated_at = ? WHERE id = ?")) {
                statement.setLong(1, item.getCategoryId());
                statement.setString(2, item.getTitle());
                statement.setString(3, item.getDescription());
                statement.setString(4, item.getPrice().toPlainString());
                statement.setString(5, item.getTradeType().code());
                statement.setString(6, item.getCondition().code());
                statement.setString(7, String.join(Validators.TAG_SEPARATOR, item.getTags()));
                statement.setString(8, toDbTime(item.getUpdatedAt()));
                statement.setLong(9, item.getId());
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean updateStatus(long itemId, ItemStatus status) {
        if (status == null) {
            throw new DaoException("更新物品状态失败：状态为空");
        }
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("UPDATE items SET status = ?, updated_at = ? WHERE id = ?")) {
                statement.setString(1, status.code());
                statement.setString(2, toDbTime(java.time.LocalDateTime.now()));
                statement.setLong(3, itemId);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public List<Item> findByOwner(long ownerId) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " + COLUMNS + " FROM items WHERE owner_id = ? ORDER BY created_at DESC, id DESC")) {
                statement.setLong(1, ownerId);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Item> items = new ArrayList<Item>();
                    while (rows.next()) {
                        items.add(map(rows));
                    }
                    return items;
                }
            }
        });
    }

    @Override
    public boolean deleteById(long itemId) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("DELETE FROM items WHERE id = ?")) {
                statement.setLong(1, itemId);
                return statement.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean addFavorite(long userId, long itemId) {
        return inTransaction(connection -> {
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT OR IGNORE INTO favorites (user_id, item_id, created_at) VALUES (?, ?, ?)")) {
                insert.setLong(1, userId);
                insert.setLong(2, itemId);
                insert.setString(3, toDbTime(java.time.LocalDateTime.now()));
                if (insert.executeUpdate() == 0) {
                    return Boolean.FALSE;
                }
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE items SET favorite_count = favorite_count + 1 WHERE id = ?")) {
                update.setLong(1, itemId);
                if (update.executeUpdate() == 0) {
                    throw new DaoException("收藏失败：物品不存在 id=" + itemId);
                }
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public boolean removeFavorite(long userId, long itemId) {
        return inTransaction(connection -> {
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM favorites WHERE user_id = ? AND item_id = ?")) {
                delete.setLong(1, userId);
                delete.setLong(2, itemId);
                if (delete.executeUpdate() == 0) {
                    return Boolean.FALSE;
                }
            }
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE items SET favorite_count = MAX(favorite_count - 1, 0) WHERE id = ?")) {
                update.setLong(1, itemId);
                update.executeUpdate();
            }
            return Boolean.TRUE;
        });
    }

    @Override
    public boolean isFavorite(long userId, long itemId) {
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM favorites WHERE user_id = ? AND item_id = ?")) {
                statement.setLong(1, userId);
                statement.setLong(2, itemId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next();
                }
            }
        });
    }

    @Override
    public List<Item> findFavorites(long userId) {
        String sql = "SELECT i.id, i.owner_id, i.category_id, i.title, i.description, i.price, "
                + "i.trade_type, i.status, i.condition_level, i.tags, i.favorite_count, "
                + "i.created_at, i.updated_at FROM items i "
                + "INNER JOIN favorites f ON f.item_id = i.id "
                + "WHERE f.user_id = ? ORDER BY f.created_at DESC, i.id DESC";
        return withConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Item> items = new ArrayList<Item>();
                    while (rows.next()) {
                        items.add(map(rows));
                    }
                    return items;
                }
            }
        });
    }

    @Override
    public int countFavorites(long userId) {
        return withConnection(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement("SELECT COUNT(*) FROM favorites WHERE user_id = ?")) {
                statement.setLong(1, userId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getInt(1) : 0;
                }
            }
        });
    }

    private String buildWhere(ItemQuery query, List<Object> parameters) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (query.getStatus() != null) {
            where.append(" AND status = ?");
            parameters.add(query.getStatus().code());
        } else {
            // 默认只看在架/交易中的物品，已下架与已完成不出现在检索结果中
            where.append(" AND status IN ('ON_SALE', 'LOCKED')");
        }
        if (query.getCategoryId() != null) {
            where.append(" AND category_id = ?");
            parameters.add(query.getCategoryId());
        }
        if (query.getOwnerId() != null) {
            where.append(" AND owner_id = ?");
            parameters.add(query.getOwnerId());
        }
        if (query.getTradeType() != null) {
            where.append(" AND trade_type = ?");
            parameters.add(query.getTradeType().code());
        }
        if (query.getMinPrice() != null) {
            where.append(" AND price >= ?");
            parameters.add(query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            where.append(" AND price <= ?");
            parameters.add(query.getMaxPrice());
        }
        if (query.getKeyword() != null) {
            String pattern = likePattern(query.getKeyword());
            where.append(" AND (LOWER(title) LIKE LOWER(?) ESCAPE '\\'"
                    + " OR LOWER(description) LIKE LOWER(?) ESCAPE '\\'"
                    + " OR LOWER(tags) LIKE LOWER(?) ESCAPE '\\')");
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        return where.toString();
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object value = parameters.get(index);
            int position = index + 1;
            if (value instanceof BigDecimal) {
                statement.setString(position, ((BigDecimal) value).toPlainString());
            } else if (value instanceof Long) {
                statement.setLong(position, (Long) value);
            } else if (value instanceof Integer) {
                statement.setInt(position, (Integer) value);
            } else {
                statement.setString(position, String.valueOf(value));
            }
        }
    }

    private static Item map(ResultSet rows) throws SQLException {
        String tradeType = rows.getString("trade_type");
        String status = rows.getString("status");
        String condition = rows.getString("condition_level");
        return Item.builder()
                .id(rows.getLong("id"))
                .ownerId(rows.getLong("owner_id"))
                .categoryId(rows.getLong("category_id"))
                .title(rows.getString("title"))
                .description(rows.getString("description"))
                .price(new BigDecimal(rows.getString("price")))
                .tradeType(TradeType.fromCode(tradeType.toUpperCase(Locale.ROOT)))
                .status(ItemStatus.fromCode(status.toUpperCase(Locale.ROOT)))
                .condition(ItemCondition.fromCode(condition.toUpperCase(Locale.ROOT)))
                .tags(Validators.splitTags(rows.getString("tags")))
                .favoriteCount(rows.getInt("favorite_count"))
                .createdAt(fromDbTime(rows.getString("created_at")))
                .updatedAt(fromDbTime(rows.getString("updated_at")))
                .build();
    }
}
