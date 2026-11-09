package com.campus.lowcarbon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.Passwords;
import com.campus.lowcarbon.common.PermissionException;
import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.dao.ItemQuery;
import com.campus.lowcarbon.domain.Category;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemCondition;
import com.campus.lowcarbon.domain.ItemStatus;
import com.campus.lowcarbon.domain.TradeType;
import com.campus.lowcarbon.domain.User;
import com.campus.lowcarbon.support.InMemoryCategoryDao;
import com.campus.lowcarbon.support.InMemoryItemDao;
import com.campus.lowcarbon.support.InMemoryUserDao;

/** ItemService 单元测试：内存 DAO + 真实领域对象。 */
@DisplayName("ItemService 物品业务")
class ItemServiceTest {

    private InMemoryUserDao userDao;
    private InMemoryItemDao itemDao;
    private InMemoryCategoryDao categoryDao;
    private UserService userService;
    private CategoryService categoryService;
    private ItemService itemService;
    private User owner;
    private User other;
    private Category category;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        itemDao = new InMemoryItemDao();
        categoryDao = new InMemoryCategoryDao();
        Passwords passwords = new Passwords(1000);
        userService = new UserService(userDao, passwords);
        categoryService = new CategoryService(categoryDao);
        itemService = new ItemService(userDao, itemDao, categoryDao);

        owner = userService.register("owner01", "abc12345", "");
        other = userService.register("other01", "abc12345", "");
        category = categoryService.create("教材书籍", "课本", 10);
    }

    private ItemDraft.Builder draft() {
        return ItemDraft.builder()
                .categoryId(category.getId())
                .title("数据结构教材")
                .description("九成新")
                .price(new BigDecimal("25.00"))
                .tradeType(TradeType.SELL)
                .condition(ItemCondition.LIKE_NEW)
                .tags("教材,计算机");
    }

    @Test
    @DisplayName("发布成功：状态在架、标签规范化")
    void publishCreatesOnSaleItem() {
        Item item = itemService.publish(owner.getId(), draft().build());
        assertTrue(item.getId() > 0);
        assertEquals(ItemStatus.ON_SALE, item.getStatus());
        assertEquals(owner.getId(), item.getOwnerId());
        assertEquals("教材", item.getTags().get(0));
        assertEquals(1, itemService.totalCount());
    }

    @Test
    @DisplayName("发布校验：分类不存在、分类被禁用、用户被封禁")
    void publishValidations() {
        assertThrows(NotFoundException.class, () -> itemService.publish(owner.getId(), draft().categoryId(999).build()));
        assertThrows(NotFoundException.class, () -> itemService.publish(999L, draft().build()));

        categoryService.setEnabled(category.getId(), false);
        ValidationException disabled = assertThrows(ValidationException.class,
                () -> itemService.publish(owner.getId(), draft().build()));
        assertTrue(disabled.getMessage().contains("禁用"));

        categoryService.setEnabled(category.getId(), true);
        userService.setBanned(other.getId(), true);
        assertThrows(com.campus.lowcarbon.common.AuthException.class,
                () -> itemService.publish(other.getId(), draft().build()));
    }

    @Test
    @DisplayName("只有发布者能修改、下架、删除")
    void ownerOnlyOperations() {
        Item item = itemService.publish(owner.getId(), draft().build());
        assertThrows(PermissionException.class,
                () -> itemService.update(other.getId(), item.getId(), draft().title("被篡改").build()));
        assertThrows(PermissionException.class, () -> itemService.offShelf(other.getId(), item.getId()));
        assertThrows(PermissionException.class, () -> itemService.remove(other.getId(), item.getId()));

        Item updated = itemService.update(owner.getId(), item.getId(), draft().title("新标题").build());
        assertEquals("新标题", updated.getTitle());
    }

    @Test
    @DisplayName("上下架状态机生效，下架后不出现在检索结果")
    void offShelfHidesItem() {
        Item item = itemService.publish(owner.getId(), draft().build());
        assertEquals(1, itemService.count(ItemQuery.all()));

        Item off = itemService.offShelf(owner.getId(), item.getId());
        assertEquals(ItemStatus.OFF_SHELF, off.getStatus());
        assertEquals(0, itemService.count(ItemQuery.all()));
        assertEquals(1, itemService.myItems(owner.getId()).size());

        Item relisted = itemService.relist(owner.getId(), item.getId());
        assertEquals(ItemStatus.ON_SALE, relisted.getStatus());
        assertEquals(1, itemService.count(ItemQuery.all()));
    }

    @Test
    @DisplayName("交易中的物品不能删除")
    void cannotRemoveLockedItem() {
        Item item = itemService.publish(owner.getId(), draft().build());
        itemDao.updateStatus(item.getId(), ItemStatus.LOCKED);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> itemService.remove(owner.getId(), item.getId()));
        assertTrue(ex.getMessage().contains("交易中"));
        assertEquals(1, itemService.totalCount());
    }

    @Test
    @DisplayName("收藏：不能收藏自己的物品，重复收藏返回 false，计数一致")
    void favoriteRules() {
        Item item = itemService.publish(owner.getId(), draft().build());
        assertThrows(ValidationException.class, () -> itemService.favorite(owner.getId(), item.getId()));

        assertTrue(itemService.favorite(other.getId(), item.getId()));
        assertFalse(itemService.favorite(other.getId(), item.getId()));
        assertEquals(1, itemService.favoriteCount(other.getId()));
        assertEquals(1, itemService.favorites(other.getId()).size());

        assertTrue(itemService.unfavorite(other.getId(), item.getId()));
        assertFalse(itemService.unfavorite(other.getId(), item.getId()));
        assertEquals(0, itemService.favoriteCount(other.getId()));
    }

    @Test
    @DisplayName("下架物品不能收藏，删除后查询抛 NotFoundException")
    void favoriteRequiresVisibleItem() {
        Item item = itemService.publish(owner.getId(), draft().build());
        itemService.offShelf(owner.getId(), item.getId());
        assertThrows(ValidationException.class, () -> itemService.favorite(other.getId(), item.getId()));

        itemService.remove(owner.getId(), item.getId());
        assertThrows(NotFoundException.class, () -> itemService.requireItem(item.getId()));
    }

    @Test
    @DisplayName("检索支持关键词与价格区间")
    void searchByKeywordAndPrice() {
        itemService.publish(owner.getId(), draft().title("高数教材").price(new BigDecimal("10.00")).build());
        itemService.publish(owner.getId(), draft().title("机械键盘").price(new BigDecimal("199.00")).build());

        assertEquals(2, itemService.search(ItemQuery.all()).size());
        assertEquals(1, itemService.search(ItemQuery.builder().keyword("键盘").build()).size());
        assertEquals(1, itemService.search(ItemQuery.builder()
                .priceRange(new BigDecimal("0"), new BigDecimal("50")).build()).size());
        assertEquals(1, itemService.search(ItemQuery.builder()
                .priceRange(new BigDecimal("100"), null).build()).size());
    }
}
