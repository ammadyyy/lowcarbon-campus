package com.campus.lowcarbon.support;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import com.campus.lowcarbon.common.Passwords;
import com.campus.lowcarbon.dao.DaoFactory;
import com.campus.lowcarbon.dao.Database;
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
 * 测试夹具：每个测试用独立的临时 SQLite 文件，互不影响。
 */
public final class TestContext {

    /** 测试用弱迭代次数，避免每次散列耗时过长（生产仍是 120000）。 */
    public static final int TEST_ITERATIONS = 1000;

    private final Database database;
    private final DaoFactory daoFactory;
    private final UserService userService;
    private final ItemService itemService;
    private final CategoryService categoryService;

    public TestContext(Path directory) {
        Path file = directory.resolve("campus-test.db");
        String url = "jdbc:sqlite:" + file.toAbsolutePath().toString().replace('\\', '/');
        this.database = new Database(url);
        this.database.initialize();
        this.daoFactory = new DaoFactory(database);
        this.userService = new UserService(daoFactory.userDao(), new Passwords(TEST_ITERATIONS));
        this.categoryService = new CategoryService(daoFactory.categoryDao());
        this.itemService = new ItemService(daoFactory.userDao(), daoFactory.itemDao(), daoFactory.categoryDao());
    }

    public Database database() {
        return database;
    }

    public DaoFactory daoFactory() {
        return daoFactory;
    }

    public UserService userService() {
        return userService;
    }

    public ItemService itemService() {
        return itemService;
    }

    public CategoryService categoryService() {
        return categoryService;
    }

    /** 建一个学生账号。 */
    public User newStudent(String username) {
        return userService.register(username, "passw0rd1", "13800000000");
    }

    /** 取一个启用分类，没有就创建。 */
    public Category firstCategory() {
        List<Category> categories = categoryService.listEnabled();
        if (!categories.isEmpty()) {
            return categories.get(0);
        }
        return categoryService.create("测试分类", "单元测试用", 1);
    }

    /** 发布一件物品。 */
    public Item newItem(User owner, String title, String price) {
        return itemService.publish(owner.getId(), ItemDraft.builder()
                .categoryId(firstCategory().getId())
                .title(title)
                .description("单元测试物品：" + title)
                .price(new BigDecimal(price))
                .tradeType(TradeType.SELL)
                .condition(ItemCondition.GOOD)
                .tags("测试,闲置")
                .build());
    }
}
