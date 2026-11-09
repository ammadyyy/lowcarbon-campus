package com.campus.lowcarbon.dao;

/**
 * 物品列表排序策略（策略模式）。
 * <p>
 * 排序规则被封装为独立对象，DAO 只负责拼接子句；将来新增"按距离""按信用分"等排序
 * 只需增加一个实现，不必修改 {@link ItemDao} 的任何代码（开闭原则）。
 * </p>
 */
public interface ItemSortStrategy {

    /** 策略显示名，供 GUI 下拉框使用。 */
    String label();

    /**
     * ORDER BY 子句（不含 "ORDER BY" 关键字本身）。
     * <p>实现必须是常量 SQL，禁止拼接用户输入，防止 SQL 注入。</p>
     */
    String orderByClause();
}
