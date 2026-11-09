package com.campus.lowcarbon.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.campus.lowcarbon.common.PermissionException;
import com.campus.lowcarbon.common.ValidationException;

/** Item 实体单元测试：构造校验、权限、状态机。 */
@DisplayName("Item 实体")
class ItemDomainTest {

    private Item.Builder validBuilder() {
        return Item.builder()
                .ownerId(7L)
                .categoryId(1L)
                .title("数据结构教材")
                .description("九成新，无笔记")
                .price(new BigDecimal("25.00"))
                .tradeType(TradeType.SELL)
                .condition(ItemCondition.LIKE_NEW)
                .tags(Arrays.asList("教材", "计算机"));
    }

    @Test
    @DisplayName("合法物品默认在架、收藏数 0")
    void buildValidItem() {
        Item item = validBuilder().build();
        assertEquals(ItemStatus.ON_SALE, item.getStatus());
        assertEquals(0, item.getFavoriteCount());
        assertTrue(item.isVisible());
        assertTrue(item.isOwnedBy(7L));
        assertFalse(item.isOwnedBy(8L));
        assertEquals(Arrays.asList("教材", "计算机"), item.getTags());
    }

    @Test
    @DisplayName("缺少发布者/分类/标题，或价格、标题越界均被拒绝")
    void rejectInvalidItem() {
        assertThrows(ValidationException.class, () -> validBuilder().ownerId(0).build());
        assertThrows(ValidationException.class, () -> validBuilder().categoryId(0).build());
        assertThrows(ValidationException.class, () -> validBuilder().title("  ").build());
        assertThrows(ValidationException.class, () -> validBuilder().title(repeat("题", 51)).build());
        assertThrows(ValidationException.class, () -> validBuilder().description(repeat("描", 501)).build());
        assertThrows(ValidationException.class, () -> validBuilder().price(new BigDecimal("-1")).build());
        assertThrows(ValidationException.class, () -> validBuilder().price(new BigDecimal("1.999")).build());
        assertThrows(ValidationException.class, () -> validBuilder().tradeType(null).build());
        assertThrows(ValidationException.class, () -> validBuilder().condition(null).build());
    }

    @Test
    @DisplayName("只有发布者能修改，且交易中/已完成不可编辑")
    void ownershipAndEditability() {
        Item item = validBuilder().build();
        assertEquals(item, item.ensureOwnedBy(7L));
        assertThrows(PermissionException.class, () -> item.ensureOwnedBy(8L));
        assertThrows(PermissionException.class, () -> item.ensureEditableBy(8L));

        Item locked = item.changeStatus(ItemStatus.LOCKED);
        assertThrows(ValidationException.class, () -> locked.ensureEditableBy(7L));

        Item completed = locked.changeStatus(ItemStatus.COMPLETED);
        assertThrows(ValidationException.class, () -> completed.ensureEditableBy(7L));
        assertTrue(item.ensureEditableBy(7L).isVisible());
    }

    @Test
    @DisplayName("状态机：合法迁移成功，非法迁移抛异常")
    void statusTransitions() {
        Item onSale = validBuilder().build();
        Item offShelf = onSale.changeStatus(ItemStatus.OFF_SHELF);
        assertEquals(ItemStatus.OFF_SHELF, offShelf.getStatus());
        assertFalse(offShelf.isVisible());

        Item relisted = offShelf.changeStatus(ItemStatus.ON_SALE);
        assertEquals(ItemStatus.ON_SALE, relisted.getStatus());

        Item locked = relisted.changeStatus(ItemStatus.LOCKED);
        assertEquals(ItemStatus.LOCKED, locked.getStatus());
        assertEquals(ItemStatus.ON_SALE, locked.changeStatus(ItemStatus.ON_SALE).getStatus());
        assertEquals(ItemStatus.COMPLETED, locked.changeStatus(ItemStatus.COMPLETED).getStatus());

        assertThrows(ValidationException.class, () -> onSale.changeStatus(ItemStatus.COMPLETED));
        assertThrows(ValidationException.class, () -> onSale.changeStatus(null));
        assertThrows(ValidationException.class,
                () -> locked.changeStatus(ItemStatus.COMPLETED).changeStatus(ItemStatus.ON_SALE));
    }

    @Test
    @DisplayName("收藏数、发布者可变副本不影响原对象")
    void copyWithChanges() {
        Item item = validBuilder().id(10).build();
        Item counted = item.withFavoriteCount(5);
        assertEquals(0, item.getFavoriteCount());
        assertEquals(5, counted.getFavoriteCount());
        assertEquals(99L, item.withOwner(99L).getOwnerId());
        assertTrue(item.summary().startsWith("#10 数据结构教材"));
    }

    private static String repeat(String text, int times) {
        StringBuilder builder = new StringBuilder(text.length() * times);
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return builder.toString();
    }
}
