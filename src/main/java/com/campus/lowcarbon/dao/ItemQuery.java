package com.campus.lowcarbon.dao;

import java.math.BigDecimal;

import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.domain.ItemStatus;
import com.campus.lowcarbon.domain.TradeType;

/**
 * 物品检索条件（查询对象 + Builder 模式）。
 * <p>
 * 把"可选的筛选条件"收敛为一个值对象，避免 Service 层出现 8 个参数的长方法，
 * 也避免 DAO 里拼接 SQL 时漏掉条件。
 * </p>
 */
public final class ItemQuery {

    /** 单页最大条数，防止一次拉取过多数据拖垮界面。 */
    public static final int MAX_LIMIT = 200;

    private final String keyword;
    private final Long categoryId;
    private final BigDecimal minPrice;
    private final BigDecimal maxPrice;
    private final ItemStatus status;
    private final Long ownerId;
    private final TradeType tradeType;
    private final ItemSortStrategy sort;
    private final int limit;
    private final int offset;

    private ItemQuery(Builder builder) {
        this.keyword = builder.keyword;
        this.categoryId = builder.categoryId;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.status = builder.status;
        this.ownerId = builder.ownerId;
        this.tradeType = builder.tradeType;
        this.sort = builder.sort;
        this.limit = builder.limit;
        this.offset = builder.offset;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 默认条件：所有在架物品，最新发布在前。 */
    public static ItemQuery all() {
        return builder().build();
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public ItemSortStrategy getSort() {
        return sort;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public Builder toBuilder() {
        return new Builder()
                .keyword(keyword)
                .categoryId(categoryId)
                .priceRange(minPrice, maxPrice)
                .status(status)
                .ownerId(ownerId)
                .tradeType(tradeType)
                .sort(sort)
                .limit(limit)
                .offset(offset);
    }

    /** 检索条件构造器。 */
    public static final class Builder {

        private String keyword;
        private Long categoryId;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private ItemStatus status;
        private Long ownerId;
        private TradeType tradeType;
        private ItemSortStrategy sort = SortOrder.NEWEST;
        private int limit = 50;
        private int offset = 0;

        public Builder keyword(String keyword) {
            this.keyword = keyword;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder priceRange(BigDecimal min, BigDecimal max) {
            this.minPrice = min;
            this.maxPrice = max;
            return this;
        }

        public Builder status(ItemStatus status) {
            this.status = status;
            return this;
        }

        public Builder ownerId(Long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder tradeType(TradeType tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public Builder sort(ItemSortStrategy sort) {
            this.sort = sort;
            return this;
        }

        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        public ItemQuery build() {
            String normalizedKeyword = null;
            if (keyword != null && !keyword.trim().isEmpty()) {
                normalizedKeyword = keyword.trim();
                if (normalizedKeyword.length() > 50) {
                    throw new ValidationException("搜索关键词不能超过 50 个字符");
                }
            }
            if (minPrice != null && minPrice.signum() < 0) {
                throw new ValidationException("最低价格不能为负数");
            }
            if (maxPrice != null && maxPrice.signum() < 0) {
                throw new ValidationException("最高价格不能为负数");
            }
            if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
                throw new ValidationException("最低价格不能高于最高价格");
            }
            if (limit < 1 || limit > MAX_LIMIT) {
                throw new ValidationException("每页条数必须在 1 到 " + MAX_LIMIT + " 之间");
            }
            if (offset < 0) {
                throw new ValidationException("分页偏移量不能为负数");
            }
            if (sort == null) {
                sort = SortOrder.NEWEST;
            }
            this.keyword = normalizedKeyword;
            return new ItemQuery(this);
        }
    }
}
