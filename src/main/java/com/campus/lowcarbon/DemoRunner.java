package com.campus.lowcarbon;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.campus.lowcarbon.dao.ItemQuery;
import com.campus.lowcarbon.dao.SortOrder;
import com.campus.lowcarbon.domain.Category;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemCondition;
import com.campus.lowcarbon.domain.TradeType;
import com.campus.lowcarbon.domain.User;
import com.campus.lowcarbon.service.CategoryService;
import com.campus.lowcarbon.service.ItemDraft;
import com.campus.lowcarbon.service.ItemService;
import com.campus.lowcarbon.service.UserService;

/**
 * 迭代二的验收演示脚本（无界面，可在控制台/CI 中运行）。
 * <p>
 * 覆盖：注册与登录、口令加密入库、发布物品、检索与筛选、收藏事务、
 * 权限校验（他人不能修改）、上下架状态机。执行 {@code java -jar xxx.jar demo} 触发。
 * </p>
 */
public final class DemoRunner {

    private static final String DEMO_OWNER = "demo_owner";
    private static final String DEMO_BUYER = "demo_buyer";
    private static final String DEMO_PASSWORD = "demo1234";

    private final UserService userService;
    private final ItemService itemService;
    private final CategoryService categoryService;

    public DemoRunner(UserService userService, ItemService itemService, CategoryService categoryService) {
        this.userService = userService;
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    /** 执行完整演示，输出关键结果。 */
    public void run() {
        section("1. 账号模块：注册 / 登录 / 口令散列");
        User owner = ensureUser(DEMO_OWNER, "13800000001");
        User buyer = ensureUser(DEMO_BUYER, "13800000002");
        System.out.println("注册/复用用户：" + owner.getUsername() + "（信用分 " + owner.getCreditScore() + "）");
        System.out.println("数据库中的口令字段（前 40 字符）："
                + owner.getPasswordHash().substring(0, Math.min(40, owner.getPasswordHash().length())) + "...");
        System.out.println("口令是否为 PBKDF2 编码串：" + com.campus.lowcarbon.common.Passwords.isEncoded(owner.getPasswordHash()));
        User logged = userService.login(DEMO_OWNER, DEMO_PASSWORD);
        System.out.println("登录成功：" + logged.getUsername() + "，角色 " + logged.getRole().label());

        section("2. 物品模块：发布（含分类、成色、标签校验）");
        if (itemService.myItems(owner.getId()).isEmpty()) {
            Category book = requireCategory("教材书籍");
            Category digital = requireCategory("数码配件");
            Category life = requireCategory("生活物资");

            Item published = itemService.publish(owner.getId(), ItemDraft.builder()
                    .categoryId(book.getId())
                    .title("《数据结构与算法分析》第三版")
                    .description("仅翻阅，无笔记无划线，配套习题答案")
                    .price(new BigDecimal("25.00"))
                    .tradeType(TradeType.SELL)
                    .condition(ItemCondition.LIKE_NEW)
                    .tags("教材,计算机,九成新")
                    .build());
            itemService.publish(owner.getId(), ItemDraft.builder()
                    .categoryId(digital.getId())
                    .title("65W 氮化镓充电器")
                    .description("多口快充，宿舍桌面必备")
                    .price(new BigDecimal("45.50"))
                    .tradeType(TradeType.SELL)
                    .condition(ItemCondition.GOOD)
                    .tags("数码,充电器")
                    .build());
            itemService.publish(owner.getId(), ItemDraft.builder()
                    .categoryId(life.getId())
                    .title("闲置折叠收纳箱（免费赠送）")
                    .description("搬宿舍剩余，自取")
                    .price(BigDecimal.ZERO)
                    .tradeType(TradeType.DONATE)
                    .condition(ItemCondition.FAIR)
                    .tags("收纳,免费")
                    .build());
            System.out.println("已发布 3 件物品，首件编号：" + published.getId());
        } else {
            System.out.println("演示物品已存在，跳过发布（保证脚本可重复执行）");
        }

        section("3. 检索模块：关键词 + 价格区间 + 排序");
        List<Item> all = itemService.search(ItemQuery.builder().build());
        System.out.println("全部在架物品：" + all.size() + " 件");
        print(all);

        List<Item> cheap = itemService.search(ItemQuery.builder()
                .priceRange(new BigDecimal("0"), new BigDecimal("30"))
                .sort(SortOrder.PRICE_ASC)
                .build());
        System.out.println("\n价格 0-30 元（价格升序）：");
        print(cheap);

        List<Item> keyword = itemService.search(ItemQuery.builder().keyword("充电").build());
        System.out.println("\n关键词“充电”命中 " + keyword.size() + " 件：");
        print(keyword);

        section("4. 收藏模块：事务保证收藏关系与计数一致");
        if (!all.isEmpty()) {
            Item target = all.get(0);
            boolean added = itemService.favorite(buyer.getId(), target.getId());
            System.out.println("收藏结果新增=" + added + "，当前收藏数="
                    + itemService.requireItem(target.getId()).getFavoriteCount());
            System.out.println("我的收藏：" + itemService.favoriteCount(buyer.getId()) + " 件");
        }

        section("5. 权限与状态机：越权修改被拒绝、下架生效");
        if (!all.isEmpty()) {
            Item target = all.get(0);
            try {
                itemService.offShelf(buyer.getId(), target.getId());
                System.out.println("异常：非发布者竟然下架成功");
            } catch (RuntimeException ex) {
                System.out.println("越权下架被拒绝：" + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
            Item off = itemService.offShelf(owner.getId(), target.getId());
            System.out.println("发布者下架成功，新状态：" + off.getStatus().label());
            ItemQuery visible = ItemQuery.builder().keyword(target.getTitle()).build();
            System.out.println("下架后再检索同名物品：" + itemService.count(visible) + " 件（应为 0）");
            itemService.relist(owner.getId(), target.getId());
            System.out.println("重新上架后检索：" + itemService.count(visible) + " 件（应为 1）");
        }

        section("6. 数据统计");
        System.out.println("用户总数：" + userService.count()
                + "，物品总数：" + itemService.totalCount()
                + "，分类数：" + categoryService.count());
    }

    private User ensureUser(String username, String contact) {
        Optional<User> existing = userService.findByUsername(username);
        if (existing.isPresent()) {
            return userService.login(username, DEMO_PASSWORD);
        }
        return userService.register(username, DEMO_PASSWORD, contact);
    }

    private Category requireCategory(String name) {
        for (Category category : categoryService.listAll()) {
            if (category.getName().equals(name)) {
                return category;
            }
        }
        return categoryService.create(name, "", 60);
    }

    private static void print(List<Item> items) {
        for (Item item : items) {
            System.out.println("  - " + item.summary());
        }
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("===== " + title + " =====");
    }
}
