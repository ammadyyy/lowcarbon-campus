package com.campus.lowcarbon.dao;

import java.util.List;
import java.util.Optional;

import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemStatus;

/** 闲置物品数据访问接口。 */
public interface ItemDao {

    Item insert(Item item);

    Optional<Item> findById(long id);

    /** 组合条件检索。 */
    List<Item> search(ItemQuery query);

    /** 满足条件的总条数（分页时显示总页数）。 */
    int count(ItemQuery query);

    /** 全量更新可修改字段（不包括收藏数）。 */
    boolean update(Item item);

    /** 仅更新状态与更新时间。 */
    boolean updateStatus(long itemId, ItemStatus status);

    /** 发布者自己的物品（含已下架、已完成），按发布时间倒序。 */
    List<Item> findByOwner(long ownerId);

    boolean deleteById(long itemId);

    /**
     * 收藏。已收藏时返回 false（幂等）。
     */
    boolean addFavorite(long userId, long itemId);

    /** 取消收藏。未收藏时返回 false。 */
    boolean removeFavorite(long userId, long itemId);

    boolean isFavorite(long userId, long itemId);

    /** 某用户收藏的物品，最近收藏在前。 */
    List<Item> findFavorites(long userId);

    /** 收藏总数（统计用）。 */
    int countFavorites(long userId);
}
