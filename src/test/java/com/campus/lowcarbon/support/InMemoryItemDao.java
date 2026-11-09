package com.campus.lowcarbon.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.campus.lowcarbon.common.DaoException;
import com.campus.lowcarbon.dao.ItemDao;
import com.campus.lowcarbon.dao.ItemQuery;
import com.campus.lowcarbon.dao.SortOrder;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemStatus;

/** 内存版物品 DAO：实现与 JDBC 版本一致的筛选语义，供 Service 层单元测试使用。 */
public class InMemoryItemDao implements ItemDao {

    private final Map<Long, Item> storage = new LinkedHashMap<Long, Item>();
    private final Map<Long, Set<Long>> favorites = new LinkedHashMap<Long, Set<Long>>();
    private long sequence;

    @Override
    public Item insert(Item item) {
        long id = ++sequence;
        Item saved = item.toBuilder().id(id).build();
        storage.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Item> search(ItemQuery query) {
        List<Item> matched = new ArrayList<Item>();
        for (Item item : storage.values()) {
            if (matches(item, query)) {
                matched.add(withFavoriteCount(item));
            }
        }
        sort(matched, query.getSort());
        int from = Math.min(query.getOffset(), matched.size());
        int to = Math.min(from + query.getLimit(), matched.size());
        return new ArrayList<Item>(matched.subList(from, to));
    }

    @Override
    public int count(ItemQuery query) {
        int total = 0;
        for (Item item : storage.values()) {
            if (matches(item, query)) {
                total++;
            }
        }
        return total;
    }

    private boolean matches(Item item, ItemQuery query) {
        if (query.getStatus() != null) {
            if (item.getStatus() != query.getStatus()) {
                return false;
            }
        } else if (!item.isVisible()) {
            return false;
        }
        if (query.getCategoryId() != null && item.getCategoryId() != query.getCategoryId().longValue()) {
            return false;
        }
        if (query.getOwnerId() != null && item.getOwnerId() != query.getOwnerId().longValue()) {
            return false;
        }
        if (query.getTradeType() != null && item.getTradeType() != query.getTradeType()) {
            return false;
        }
        if (query.getMinPrice() != null && item.getPrice().compareTo(query.getMinPrice()) < 0) {
            return false;
        }
        if (query.getMaxPrice() != null && item.getPrice().compareTo(query.getMaxPrice()) > 0) {
            return false;
        }
        if (query.getKeyword() != null) {
            String keyword = query.getKeyword().toLowerCase(Locale.ROOT);
            String haystack = (item.getTitle() + " " + item.getDescription() + " "
                    + String.join(",", item.getTags())).toLowerCase(Locale.ROOT);
            return haystack.contains(keyword);
        }
        return true;
    }

    private void sort(List<Item> items, final com.campus.lowcarbon.dao.ItemSortStrategy sort) {
        if (sort == SortOrder.PRICE_ASC) {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item left, Item right) {
                    return left.getPrice().compareTo(right.getPrice());
                }
            });
        } else if (sort == SortOrder.PRICE_DESC) {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item left, Item right) {
                    return right.getPrice().compareTo(left.getPrice());
                }
            });
        } else if (sort == SortOrder.POPULAR) {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item left, Item right) {
                    return Integer.compare(right.getFavoriteCount(), left.getFavoriteCount());
                }
            });
        } else {
            Collections.sort(items, new Comparator<Item>() {
                @Override
                public int compare(Item left, Item right) {
                    return right.getCreatedAt().compareTo(left.getCreatedAt());
                }
            });
        }
    }

    @Override
    public boolean update(Item item) {
        if (!storage.containsKey(item.getId())) {
            return false;
        }
        storage.put(item.getId(), item);
        return true;
    }

    @Override
    public boolean updateStatus(long itemId, ItemStatus status) {
        Item item = storage.get(itemId);
        if (item == null) {
            return false;
        }
        storage.put(itemId, item.toBuilder().status(status).build());
        return true;
    }

    @Override
    public List<Item> findByOwner(long ownerId) {
        List<Item> result = new ArrayList<Item>();
        for (Item item : storage.values()) {
            if (item.getOwnerId() == ownerId) {
                result.add(withFavoriteCount(item));
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public boolean deleteById(long itemId) {
        return storage.remove(itemId) != null;
    }

    @Override
    public boolean addFavorite(long userId, long itemId) {
        if (!storage.containsKey(itemId)) {
            throw new DaoException("收藏失败：物品不存在 id=" + itemId);
        }
        Set<Long> items = favorites.get(userId);
        if (items == null) {
            items = new LinkedHashSet<Long>();
            favorites.put(userId, items);
        }
        return items.add(itemId);
    }

    @Override
    public boolean removeFavorite(long userId, long itemId) {
        Set<Long> items = favorites.get(userId);
        return items != null && items.remove(itemId);
    }

    @Override
    public boolean isFavorite(long userId, long itemId) {
        Set<Long> items = favorites.get(userId);
        return items != null && items.contains(itemId);
    }

    @Override
    public List<Item> findFavorites(long userId) {
        List<Item> result = new ArrayList<Item>();
        Set<Long> items = favorites.get(userId);
        if (items == null) {
            return result;
        }
        for (Long itemId : items) {
            Item item = storage.get(itemId);
            if (item != null) {
                result.add(withFavoriteCount(item));
            }
        }
        return result;
    }

    @Override
    public int countFavorites(long userId) {
        Set<Long> items = favorites.get(userId);
        return items == null ? 0 : items.size();
    }

    private Item withFavoriteCount(Item item) {
        int count = 0;
        for (Set<Long> items : favorites.values()) {
            if (items.contains(item.getId())) {
                count++;
            }
        }
        return item.withFavoriteCount(count);
    }
}
