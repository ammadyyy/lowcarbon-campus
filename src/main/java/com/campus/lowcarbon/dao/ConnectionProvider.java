package com.campus.lowcarbon.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接提供者抽象。
 * <p>DAO 只依赖该接口而不直接使用 {@code DriverManager}，
 * 便于单元测试时注入临时数据库，也便于将来切换连接池（依赖倒置原则）。</p>
 */
public interface ConnectionProvider {

    /**
     * 获取一个数据库连接，调用方负责关闭。
     *
     * @return 连接
     * @throws SQLException 连接失败
     */
    Connection getConnection() throws SQLException;
}
