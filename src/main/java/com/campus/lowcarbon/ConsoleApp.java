package com.campus.lowcarbon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.List;

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
 * 迭代二的控制台交互界面（Swing 界面在迭代三接入，业务层无需改动）。
 * <p>启动：{@code java -jar lowcarbon-campus-0.2.0.jar}，演示：追加参数 {@code demo}。</p>
 */
public class ConsoleApp {

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final UserService userService;
    private final ItemService itemService;
    private final CategoryService categoryService;

    private User currentUser;

    public ConsoleApp(UserService userService, ItemService itemService, CategoryService categoryService) {
        this.userService = userService;
        this.itemService = itemService;
        this.categoryService = categoryService;
    }

    /** 主循环。 */
    public void start() {
        System.out.println("==============================================");
        System.out.println("  低碳校园资源循环流转系统 v0.2（迭代二）");
        System.out.println("  用户 / 物品业务模块 + SQLite 数据访问层");
        System.out.println("==============================================");
        boolean running = true;
        while (running) {
            try {
                running = currentUser == null ? guestMenu() : memberMenu();
            } catch (RuntimeException ex) {
                System.out.println("[操作失败] " + ex.getMessage());
            }
        }
        System.out.println("再见，感谢使用低碳校园系统！");
    }

    private boolean guestMenu() {
        System.out.println();
        System.out.println("1) 注册  2) 登录  0) 退出");
        String choice = read("请选择");
        if ("1".equals(choice)) {
            doRegister();
        } else if ("2".equals(choice)) {
            doLogin();
        } else if ("0".equals(choice)) {
            return false;
        } else {
            System.out.println("无效的选项，请重新输入");
        }
        return true;
    }

    private boolean memberMenu() {
        System.out.println();
        System.out.println("当前用户：" + currentUser.getUsername() + "（" + currentUser.getRole().label()
                + "，信用分 " + currentUser.getCreditScore() + "）");
        System.out.println("1) 浏览在架物品  2) 关键词检索  3) 发布物品  4) 我的物品");
        System.out.println("5) 收藏物品      6) 我的收藏    7) 修改密码  0) 退出登录");
        String choice = read("请选择");
        if ("1".equals(choice)) {
            showItems(itemService.search(ItemQuery.builder().build()));
        } else if ("2".equals(choice)) {
            doSearch();
        } else if ("3".equals(choice)) {
            doPublish();
        } else if ("4".equals(choice)) {
            doMyItems();
        } else if ("5".equals(choice)) {
            doFavorite();
        } else if ("6".equals(choice)) {
            showItems(itemService.favorites(currentUser.getId()));
        } else if ("7".equals(choice)) {
            doChangePassword();
        } else if ("0".equals(choice)) {
            currentUser = null;
        } else {
            System.out.println("无效的选项，请重新输入");
        }
        return true;
    }

    private void doRegister() {
        String username = read("用户名（3-20 位字母/数字/下划线）");
        String password = read("密码（8-64 位，需含字母与数字）");
        String contact = read("联系方式（可留空）");
        User user = userService.register(username, password, contact);
        System.out.println("注册成功，欢迎 " + user.getUsername() + "！请登录。");
    }

    private void doLogin() {
        String username = read("用户名");
        String password = read("密码");
        currentUser = userService.login(username, password);
        System.out.println("登录成功！");
    }

    private void doChangePassword() {
        String oldPassword = read("原密码");
        String newPassword = read("新密码");
        currentUser = userService.changePassword(currentUser.getId(), oldPassword, newPassword);
        System.out.println("密码修改成功。");
    }

    private void doSearch() {
        String keyword = read("关键词（回车跳过）");
        String minText = read("最低价（回车跳过）");
        String maxText = read("最高价（回车跳过）");
        ItemQuery.Builder builder = ItemQuery.builder().sort(SortOrder.NEWEST);
        if (!keyword.isEmpty()) {
            builder.keyword(keyword);
        }
        BigDecimal min = minText.isEmpty() ? null : new BigDecimal(minText);
        BigDecimal max = maxText.isEmpty() ? null : new BigDecimal(maxText);
        if (min != null || max != null) {
            builder.priceRange(min, max);
        }
        showItems(itemService.search(builder.build()));
    }

    private void doPublish() {
        showCategories();
        String categoryText = read("分类编号");
        String title = read("标题");
        String description = read("描述（可留空）");
        String priceText = read("价格（元，0 表示赠送）");
        String tradeText = read("流转方式 1)出售 2)交换 3)赠送");
        String conditionText = read("成色 1)全新 2)几乎全新 3)良好 4)一般");
        String tags = read("标签（逗号分隔，最多 5 个，可留空）");

        ItemDraft draft = ItemDraft.builder()
                .categoryId(Long.parseLong(categoryText.trim()))
                .title(title)
                .description(description)
                .price(priceText)
                .tradeType(parseTradeType(tradeText))
                .condition(parseCondition(conditionText))
                .tags(tags)
                .build();
        Item item = itemService.publish(currentUser.getId(), draft);
        System.out.println("发布成功：" + item.summary());
    }

    private void doMyItems() {
        List<Item> mine = itemService.myItems(currentUser.getId());
        if (mine.isEmpty()) {
            System.out.println("你还没有发布过物品。");
            return;
        }
        showItems(mine);
        String idText = read("输入要操作的物品编号（回车返回）");
        if (idText.isEmpty()) {
            return;
        }
        long itemId = Long.parseLong(idText.trim());
        String action = read("1) 下架  2) 重新上架  3) 删除");
        if ("1".equals(action)) {
            System.out.println("已下架：" + itemService.offShelf(currentUser.getId(), itemId).summary());
        } else if ("2".equals(action)) {
            System.out.println("已上架：" + itemService.relist(currentUser.getId(), itemId).summary());
        } else if ("3".equals(action)) {
            itemService.remove(currentUser.getId(), itemId);
            System.out.println("已删除物品 #" + itemId);
        } else {
            System.out.println("已取消操作");
        }
    }

    private void doFavorite() {
        String idText = read("要收藏的物品编号");
        boolean added = itemService.favorite(currentUser.getId(), Long.parseLong(idText.trim()));
        System.out.println(added ? "收藏成功" : "你已经收藏过该物品");
    }

    private void showItems(List<Item> items) {
        if (items.isEmpty()) {
            System.out.println("（没有符合条件的物品）");
            return;
        }
        System.out.println("共 " + items.size() + " 件：");
        for (Item item : items) {
            System.out.println("  " + item.summary() + "  成色=" + item.getCondition().label()
                    + " 收藏=" + item.getFavoriteCount() + " 标签=" + item.getTags());
        }
    }

    private void showCategories() {
        List<Category> categories = categoryService.listEnabled();
        System.out.println("可选分类：");
        for (Category category : categories) {
            System.out.println("  " + category.getId() + ") " + category.getName());
        }
    }

    private static TradeType parseTradeType(String text) {
        if ("2".equals(text.trim())) {
            return TradeType.EXCHANGE;
        }
        if ("3".equals(text.trim())) {
            return TradeType.DONATE;
        }
        return TradeType.SELL;
    }

    private static ItemCondition parseCondition(String text) {
        String value = text.trim();
        if ("1".equals(value)) {
            return ItemCondition.NEW;
        }
        if ("2".equals(value)) {
            return ItemCondition.LIKE_NEW;
        }
        if ("4".equals(value)) {
            return ItemCondition.FAIR;
        }
        return ItemCondition.GOOD;
    }

    private String read(String prompt) {
        System.out.print(prompt + "：");
        try {
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } catch (IOException ex) {
            throw new IllegalStateException("读取输入失败", ex);
        }
    }
}
