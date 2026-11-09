package com.campus.lowcarbon.dao;

import com.campus.lowcarbon.dao.jdbc.JdbcCategoryDao;
import com.campus.lowcarbon.dao.jdbc.JdbcItemDao;
import com.campus.lowcarbon.dao.jdbc.JdbcUserDao;

/**
 * DAO 工厂（工厂模式 + 单例的按需创建）。
 * <p>
 * 上层只依赖接口，通过工厂取得实现；将来若增加 MySQL 实现或加入连接池，
 * 只需替换工厂内部实现，Service 层与测试代码均无需改动。
 * </p>
 */
public final class DaoFactory {

    private final ConnectionProvider provider;

    private UserDao userDao;
    private ItemDao itemDao;
    private CategoryDao categoryDao;

    public DaoFactory(ConnectionProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("ConnectionProvider 不能为空");
        }
        this.provider = provider;
    }

    public synchronized UserDao userDao() {
        if (userDao == null) {
            userDao = new JdbcUserDao(provider);
        }
        return userDao;
    }

    public synchronized ItemDao itemDao() {
        if (itemDao == null) {
            itemDao = new JdbcItemDao(provider);
        }
        return itemDao;
    }

    public synchronized CategoryDao categoryDao() {
        if (categoryDao == null) {
            categoryDao = new JdbcCategoryDao(provider);
        }
        return categoryDao;
    }
}
