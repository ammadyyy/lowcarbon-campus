package com.campus.lowcarbon.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.common.Validators;
import com.campus.lowcarbon.dao.CategoryDao;
import com.campus.lowcarbon.dao.ItemDao;
import com.campus.lowcarbon.dao.ItemQuery;
import com.campus.lowcarbon.dao.UserDao;
import com.campus.lowcarbon.domain.Category;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemStatus;
import com.campus.lowcarbon.domain.User;

/**
 * 物品业务服务：发布、修改、上下架、检索、收藏。
 * <p>
 * 权限（只有发布者能改）与状态流转（不能从"已完成"回到"在架"）由领域对象负责，
 * 本层负责编排 DAO 调用、校验关联数据（用户是否被封禁、分类是否可用）并记录日志。
 * </p>
 */
public class ItemService {

    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final UserDao userDao;
    private final ItemDao itemDao;
    private final CategoryDao categoryDao;

    public ItemService(UserDao userDao, ItemDao itemDao, CategoryDao categoryDao) {
        if (userDao == null || itemDao == null || categoryDao == null) {
            throw new IllegalArgumentException("ItemService 依赖不能为空");
        }
        this.userDao = userDao;
        this.itemDao = itemDao;
        this.categoryDao = categoryDao;
    }

    /**
     * 发布物品。
     *
     * @param ownerId 发布者主键
     * @param draft   发布信息
     * @return 入库后的物品
     */
    public Item publish(long ownerId, ItemDraft draft) {
        if (draft == null) {
            throw new ValidationException("发布信息不能为空");
        }
        User owner = requireActiveUser(ownerId);
        Category category = requireUsableCategory(draft.getCategoryId(), null);

        Item item = Item.builder()
                .ownerId(owner.getId())
                .categoryId(category.getId())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .price(draft.getPrice())
                .tradeType(draft.getTradeType())
                .condition(draft.getCondition())
                .tags(draft.getTags())
                .status(ItemStatus.ON_SALE)
                .build();
        Item saved = itemDao.insert(item);
        log.info("物品发布成功：id={}, owner={}, title={}", saved.getId(), ownerId, saved.getTitle());
        return saved;
    }

    /**
     * 修改自己发布的物品。
     *
     * @param actorId 操作人（必须是发布者）
     */
    public Item update(long actorId, long itemId, ItemDraft draft) {
        if (draft == null) {
            throw new ValidationException("发布信息不能为空");
        }
        Item existing = requireItem(itemId);
        existing.ensureEditableBy(actorId);
        requireActiveUser(actorId);
        Category category = requireUsableCategory(draft.getCategoryId(), existing.getCategoryId());

        Item updated = existing.toBuilder()
                .categoryId(category.getId())
                .title(draft.getTitle())
                .description(draft.getDescription())
                .price(draft.getPrice())
                .tradeType(draft.getTradeType())
                .condition(draft.getCondition())
                .tags(draft.getTags())
                .updatedAt(LocalDateTime.now())
                .build();
        if (!itemDao.update(updated)) {
            throw new NotFoundException("物品不存在：" + itemId);
        }
        log.info("物品修改成功：id={}, actor={}", itemId, actorId);
        return updated;
    }

    /** 下架自己的物品，下架后不再出现在检索结果中。 */
    public Item offShelf(long actorId, long itemId) {
        return changeStatus(actorId, itemId, ItemStatus.OFF_SHELF);
    }

    /** 重新上架自己的物品。 */
    public Item relist(long actorId, long itemId) {
        return changeStatus(actorId, itemId, ItemStatus.ON_SALE);
    }

    private Item changeStatus(long actorId, long itemId, ItemStatus target) {
        Item item = requireItem(itemId);
        item.ensureOwnedBy(actorId);
        Item changed = item.changeStatus(target);
        if (!itemDao.updateStatus(itemId, target)) {
            throw new NotFoundException("物品不存在：" + itemId);
        }
        log.info("物品状态变更：id={}, {} -> {}", itemId, item.getStatus(), target);
        return changed;
    }

    /** 删除自己的物品（交易中的物品不允许删除，避免流转记录悬空）。 */
    public void remove(long actorId, long itemId) {
        Item item = requireItem(itemId);
        item.ensureOwnedBy(actorId);
        if (item.getStatus() == ItemStatus.LOCKED) {
            throw new ValidationException("交易中的物品不能删除，请先取消交易");
        }
        if (!itemDao.deleteById(itemId)) {
            throw new NotFoundException("物品不存在：" + itemId);
        }
        log.info("物品删除成功：id={}, actor={}", itemId, actorId);
    }

    /** 按主键取出物品，不存在时抛出业务异常。 */
    public Item requireItem(long itemId) {
        Validators.requireId(itemId, "物品编号");
        Optional<Item> found = itemDao.findById(itemId);
        if (!found.isPresent()) {
            throw Item.notFound(itemId);
        }
        return found.get();
    }

    /** 组合条件检索。 */
    public List<Item> search(ItemQuery query) {
        if (query == null) {
            throw new ValidationException("检索条件不能为空");
        }
        return itemDao.search(query);
    }

    /** 符合检索条件的总条数（分页用）。 */
    public int count(ItemQuery query) {
        if (query == null) {
            throw new ValidationException("检索条件不能为空");
        }
        return itemDao.count(query);
    }

    /**
     * 收藏物品。
     *
     * @return true 表示本次新增收藏，false 表示此前已收藏
     */
    public boolean favorite(long userId, long itemId) {
        requireActiveUser(userId);
        Item item = requireItem(itemId);
        if (item.isOwnedBy(userId)) {
            throw new ValidationException("不能收藏自己发布的物品");
        }
        if (!item.isVisible()) {
            throw new ValidationException("该物品已下架，无法收藏");
        }
        boolean added = itemDao.addFavorite(userId, itemId);
        if (added) {
            log.info("收藏成功：user={}, item={}", userId, itemId);
        }
        return added;
    }

    /** 取消收藏。 */
    public boolean unfavorite(long userId, long itemId) {
        Validators.requireId(userId, "用户编号");
        Validators.requireId(itemId, "物品编号");
        boolean removed = itemDao.removeFavorite(userId, itemId);
        if (removed) {
            log.info("取消收藏：user={}, item={}", userId, itemId);
        }
        return removed;
    }

    /** 我收藏的物品。 */
    public List<Item> favorites(long userId) {
        Validators.requireId(userId, "用户编号");
        return itemDao.findFavorites(userId);
    }

    /** 收藏数量。 */
    public int favoriteCount(long userId) {
        Validators.requireId(userId, "用户编号");
        return itemDao.countFavorites(userId);
    }

    /** 我发布的物品（含已下架、已完成）。 */
    public List<Item> myItems(long ownerId) {
        Validators.requireId(ownerId, "用户编号");
        return itemDao.findByOwner(ownerId);
    }

    private User requireActiveUser(long userId) {
        Validators.requireId(userId, "用户编号");
        Optional<User> found = userDao.findById(userId);
        if (!found.isPresent()) {
            throw new NotFoundException("用户不存在：" + userId);
        }
        return found.get().ensureActive();
    }

    /**
     * 分类必须存在且可用。
     *
     * @param categoryId    目标分类
     * @param currentId     物品当前分类；未更换分类时允许沿用已被禁用的旧分类
     */
    private Category requireUsableCategory(long categoryId, Long currentId) {
        Validators.requireId(categoryId, "分类编号");
        Optional<Category> found = categoryDao.findById(categoryId);
        if (!found.isPresent()) {
            throw new NotFoundException("分类不存在：" + categoryId);
        }
        Category category = found.get();
        boolean unchanged = currentId != null && currentId.longValue() == categoryId;
        if (!category.isEnabled() && !unchanged) {
            throw new ValidationException("分类《" + category.getName() + "》已被禁用，无法使用");
        }
        return category;
    }

    /** 供统计与后台使用：物品总数。 */
    public int totalCount() {
        return itemDao.count(ItemQuery.all());
    }
}
