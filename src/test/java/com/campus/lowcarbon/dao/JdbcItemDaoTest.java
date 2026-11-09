package com.campus.lowcarbon.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemStatus;
import com.campus.lowcarbon.domain.TradeType;
import com.campus.lowcarbon.domain.User;
import com.campus.lowcarbon.support.TestContext;

/** JdbcItemDao 集成测试：动态 SQL 检索、排序、分页、收藏事务。 */
@DisplayName("JdbcItemDao 物品持久化与检索")
class JdbcItemDaoTest {

    private TestContext context;
    private ItemDao itemDao;
    private User owner;
    private User buyer;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        context = new TestContext(tempDir);
        itemDao = context.daoFactory().itemDao();
        owner = context.newStudent("owner01");
        buyer = context.newStudent("buyer01");
    }

    @Test
    @DisplayName("插入后可按 id 查回，字段完整")
    void insertAndFindById() {
        Item saved = context.newItem(owner, "数据结构教材", "25.00");
        assertTrue(saved.getId() > 0);

        Item loaded = itemDao.findById(saved.getId()).get();
        assertEquals("数据结构教材", loaded.getTitle());
        assertEquals(0, loaded.getPrice().compareTo(new BigDecimal("25.00")));
        assertEquals(ItemStatus.ON_SALE, loaded.getStatus());
        assertEquals(TradeType.SELL, loaded.getTradeType());
        assertEquals(owner.getId(), loaded.getOwnerId());
        assertEquals(2, loaded.getTags().size());
        assertFalse(itemDao.findById(9999L).isPresent());
    }

    @Test
    @DisplayName("关键词对标题、描述、标签做不区分大小写的模糊匹配")
    void searchByKeyword() {
        context.newItem(owner, "Arduino 入门套件", "88.00");
        context.newItem(owner, "高等数学教材", "12.00");

        assertEquals(1, itemDao.search(ItemQuery.builder().keyword("arduino").build()).size());
        assertEquals(1, itemDao.search(ItemQuery.builder().keyword("入门").build()).size());
        assertEquals(2, itemDao.search(ItemQuery.builder().keyword("单元测试物品").build()).size());
        assertEquals(0, itemDao.search(ItemQuery.builder().keyword("不存在的词").build()).size());
        // 通配符被转义，不会命中全部
        assertEquals(0, itemDao.search(ItemQuery.builder().keyword("%").build()).size());
    }

    @Test
    @DisplayName("价格区间与流转方式筛选")
    void searchByPriceAndTradeType() {
        context.newItem(owner, "台灯", "30.00");
        context.newItem(owner, "键盘", "199.00");

        ItemQuery cheap = ItemQuery.builder()
                .priceRange(new BigDecimal("0"), new BigDecimal("50"))
                .build();
        assertEquals(1, itemDao.search(cheap).size());

        ItemQuery expensive = ItemQuery.builder().priceRange(new BigDecimal("100"), null).build();
        assertEquals(1, itemDao.search(expensive).size());

        ItemQuery exchange = ItemQuery.builder().tradeType(TradeType.EXCHANGE).build();
        assertEquals(0, itemDao.search(exchange).size());
        assertEquals(0, itemDao.count(exchange));
    }

    @Test
    @DisplayName("排序策略生效：价格升序/降序")
    void sortStrategies() {
        context.newItem(owner, "中价", "20.00");
        context.newItem(owner, "低价", "10.00");
        context.newItem(owner, "高价", "30.00");

        List<Item> ascending = itemDao.search(ItemQuery.builder().sort(SortOrder.PRICE_ASC).build());
        assertEquals("低价", ascending.get(0).getTitle());
        assertEquals("高价", ascending.get(2).getTitle());

        List<Item> descending = itemDao.search(ItemQuery.builder().sort(SortOrder.PRICE_DESC).build());
        assertEquals("高价", descending.get(0).getTitle());
    }

    @Test
    @DisplayName("分页：limit / offset 与总条数")
    void pagination() {
        for (int i = 1; i <= 5; i++) {
            context.newItem(owner, "物品" + i, String.valueOf(i * 10));
        }
        ItemQuery page = ItemQuery.builder().sort(SortOrder.PRICE_ASC).limit(2).offset(2).build();
        List<Item> items = itemDao.search(page);
        assertEquals(2, items.size());
        assertEquals("物品3", items.get(0).getTitle());
        assertEquals(5, itemDao.count(ItemQuery.all()));
    }

    @Test
    @DisplayName("默认只检索在架物品，可显式按状态检索")
    void statusFilter() {
        Item onSale = context.newItem(owner, "在架物品", "10.00");
        Item offShelf = context.newItem(owner, "下架物品", "20.00");
        itemDao.updateStatus(offShelf.getId(), ItemStatus.OFF_SHELF);

        // 未指定状态时默认过滤为"在架 + 交易中"，下架物品不可见
        assertEquals(1, itemDao.count(ItemQuery.all()));
        assertEquals(1, itemDao.count(ItemQuery.builder().ownerId(owner.getId()).build()));
        assertEquals(1, itemDao.count(ItemQuery.builder().status(ItemStatus.OFF_SHELF).build()));
        assertEquals(onSale.getId(), itemDao.search(ItemQuery.all()).get(0).getId());
        assertEquals(2, itemDao.findByOwner(owner.getId()).size());
    }

    @Test
    @DisplayName("更新字段与状态")
    void updateAndUpdateStatus() {
        Item item = context.newItem(owner, "旧标题", "10.00");
        Item changed = item.toBuilder().title("新标题").price(new BigDecimal("18.50")).build();
        assertTrue(itemDao.update(changed));

        Item reloaded = itemDao.findById(item.getId()).get();
        assertEquals("新标题", reloaded.getTitle());
        assertEquals(0, reloaded.getPrice().compareTo(new BigDecimal("18.50")));

        assertTrue(itemDao.updateStatus(item.getId(), ItemStatus.LOCKED));
        assertEquals(ItemStatus.LOCKED, itemDao.findById(item.getId()).get().getStatus());
        assertFalse(itemDao.updateStatus(9999L, ItemStatus.LOCKED));
    }

    @Test
    @DisplayName("收藏是事务操作：重复收藏幂等、计数同步、取消后归零")
    void favoriteTransaction() {
        Item item = context.newItem(owner, "被收藏物品", "10.00");

        assertTrue(itemDao.addFavorite(buyer.getId(), item.getId()));
        assertFalse(itemDao.addFavorite(buyer.getId(), item.getId()));
        assertTrue(itemDao.isFavorite(buyer.getId(), item.getId()));
        assertEquals(1, itemDao.findById(item.getId()).get().getFavoriteCount());
        assertEquals(1, itemDao.countFavorites(buyer.getId()));
        assertEquals(1, itemDao.findFavorites(buyer.getId()).size());

        assertTrue(itemDao.removeFavorite(buyer.getId(), item.getId()));
        assertFalse(itemDao.removeFavorite(buyer.getId(), item.getId()));
        assertFalse(itemDao.isFavorite(buyer.getId(), item.getId()));
        assertEquals(0, itemDao.findById(item.getId()).get().getFavoriteCount());
        assertEquals(0, itemDao.countFavorites(buyer.getId()));
    }

    @Test
    @DisplayName("删除物品同时清理收藏记录")
    void deleteRemovesItem() {
        Item item = context.newItem(owner, "待删除", "10.00");
        itemDao.addFavorite(buyer.getId(), item.getId());

        assertTrue(itemDao.deleteById(item.getId()));
        assertFalse(itemDao.deleteById(item.getId()));
        assertFalse(itemDao.findById(item.getId()).isPresent());
        assertEquals(0, itemDao.countFavorites(buyer.getId()));
    }
}
