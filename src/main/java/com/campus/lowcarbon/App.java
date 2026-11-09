package com.campus.lowcarbon;

import com.campus.lowcarbon.dao.DaoFactory;
import com.campus.lowcarbon.dao.Database;
import com.campus.lowcarbon.service.CategoryService;
import com.campus.lowcarbon.service.DataSeeder;
import com.campus.lowcarbon.service.ItemService;
import com.campus.lowcarbon.service.UserService;

/**
 * 程序入口（迭代二：控制台版本；迭代三替换为 Swing 界面时仅改这一层）。
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        Database database = Database.getInstance();
        database.initialize();

        DaoFactory factory = new DaoFactory(database);
        UserService userService = new UserService(factory.userDao());
        CategoryService categoryService = new CategoryService(factory.categoryDao());
        ItemService itemService = new ItemService(factory.userDao(), factory.itemDao(), factory.categoryDao());

        new DataSeeder(userService, categoryService).bootstrap();

        System.out.println("数据库文件：" + safePath(database));

        if (args.length > 0 && ("demo".equalsIgnoreCase(args[0]) || "--demo".equalsIgnoreCase(args[0]))) {
            new DemoRunner(userService, itemService, categoryService).run();
            return;
        }
        new ConsoleApp(userService, itemService, categoryService).start();
    }

    private static String safePath(Database database) {
        return database.getFilePath() == null ? "(内存数据库)" : database.getFilePath().toString();
    }
}
