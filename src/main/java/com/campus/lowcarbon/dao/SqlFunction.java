package com.campus.lowcarbon.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 允许抛出 SQLException 的函数式接口，配合 {@link BaseJdbcDao} 的模板方法使用。
 *
 * @param <R> 返回值类型
 */
@FunctionalInterface
public interface SqlFunction<R> {

    R apply(Connection connection) throws SQLException;
}
