package com.campus.lowcarbon.dao;

/** 内置排序策略枚举实现。 */
public enum SortOrder implements ItemSortStrategy {

    NEWEST("最新发布", "created_at DESC, id DESC"),
    PRICE_ASC("价格从低到高", "price ASC, id DESC"),
    PRICE_DESC("价格从高到低", "price DESC, id DESC"),
    POPULAR("收藏最多", "favorite_count DESC, id DESC");

    private final String label;
    private final String orderByClause;

    SortOrder(String label, String orderByClause) {
        this.label = label;
        this.orderByClause = orderByClause;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String orderByClause() {
        return orderByClause;
    }

    @Override
    public String toString() {
        return label;
    }
}
