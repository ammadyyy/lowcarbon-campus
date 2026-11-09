package com.campus.lowcarbon.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.campus.lowcarbon.common.NotFoundException;
import com.campus.lowcarbon.common.PermissionException;
import com.campus.lowcarbon.common.ValidationException;

/**
 * 闲置物品实体（不可变对象）。
 * <p>
 * ADT 不变量：标题 1-50 字；描述不超过 500 字；价格非负且最多两位小数；
 * 状态迁移必须符合 {@link ItemStatus} 状态机；只有发布者本人能修改或下架。
 * </p>
 */
public final class Item {

    public static final int TITLE_MAX = 50;
    public static final int DESCRIPTION_MAX = 500;

    private final long id;
    private final long ownerId;
    private final long categoryId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final TradeType tradeType;
    private final ItemStatus status;
    private final ItemCondition condition;
    private final List<String> tags;
    private final int favoriteCount;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Item(Builder builder) {
        this.id = builder.id;
        this.ownerId = builder.ownerId;
        this.categoryId = builder.categoryId;
        this.title = builder.title;
        this.description = builder.description;
        this.price = builder.price;
        this.tradeType = builder.tradeType;
        this.status = builder.status;
        this.condition = builder.condition;
        this.tags = Collections.unmodifiableList(new ArrayList<String>(builder.tags));
        this.favoriteCount = builder.favoriteCount;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public long getId() {
        return id;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public ItemCondition getCondition() {
        return condition;
    }

    public List<String> getTags() {
        return tags;
    }

    public int getFavoriteCount() {
        return favoriteCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(long userId) {
        return ownerId == userId;
    }

    /** 是否为可检索、可收藏的在架物品。 */
    public boolean isVisible() {
        return status.isVisible();
    }

    /**
     * 校验操作者是否为发布者本人，否则抛出越权异常。
     *
     * @throws PermissionException 不是发布者
     */
    public Item ensureOwnedBy(long actorId) {
        if (!isOwnedBy(actorId)) {
            throw new PermissionException("只能操作自己发布的物品：" + title);
        }
        return this;
    }

    /**
     * 校验物品当前是否允许编辑：必须本人操作，且未进入交易、未完成。
     *
     * @throws PermissionException 越权
     * @throws ValidationException 状态不允许编辑
     */
    public Item ensureEditableBy(long actorId) {
        ensureOwnedBy(actorId);
        if (status == ItemStatus.LOCKED) {
            throw new ValidationException("物品处于交易中，暂不能编辑");
        }
        if (status == ItemStatus.COMPLETED) {
            throw new ValidationException("物品已完成流转，不能再编辑");
        }
        return this;
    }

    /**
     * 状态迁移，返回迁移后的新实例。
     *
     * @throws ValidationException 迁移不合法
     */
    public Item changeStatus(ItemStatus target) {
        if (target == null) {
            throw new ValidationException("目标状态不能为空");
        }
        if (!status.canTransitionTo(target)) {
            throw new ValidationException(
                    "物品状态不能从「" + status.label() + "」变更为「" + target.label() + "」");
        }
        return toBuilder().status(target).updatedAt(LocalDateTime.now()).build();
    }

    public Item withFavoriteCount(int count) {
        return toBuilder().favoriteCount(count).build();
    }

    public Item withOwner(long newOwnerId) {
        return toBuilder().ownerId(newOwnerId).updatedAt(LocalDateTime.now()).build();
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .ownerId(ownerId)
                .categoryId(categoryId)
                .title(title)
                .description(description)
                .price(price)
                .tradeType(tradeType)
                .status(status)
                .condition(condition)
                .tags(tags)
                .favoriteCount(favoriteCount)
                .createdAt(createdAt)
                .updatedAt(updatedAt);
    }

    /**
     * 展示用的简要描述，供控制台与表格使用。
     */
    public String summary() {
        return "#" + id + " " + title + " [" + tradeType.label() + "/" + status.label() + "] "
                + price.toPlainString() + " 元";
    }

    /**
     * 根据 id 查找时统一使用的异常工厂。
     */
    public static NotFoundException notFound(long id) {
        return new NotFoundException("物品不存在：id=" + id);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Item)) {
            return false;
        }
        Item that = (Item) other;
        return id > 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return summary();
    }

    /** 物品构造器（Builder 模式）。 */
    public static final class Builder {

        private long id;
        private long ownerId;
        private long categoryId;
        private String title;
        private String description = "";
        private BigDecimal price = BigDecimal.ZERO;
        private TradeType tradeType = TradeType.SELL;
        private ItemStatus status = ItemStatus.ON_SALE;
        private ItemCondition condition = ItemCondition.GOOD;
        private List<String> tags = new ArrayList<String>();
        private int favoriteCount;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder ownerId(long ownerId) {
            this.ownerId = ownerId;
            return this;
        }

        public Builder categoryId(long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder tradeType(TradeType tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public Builder status(ItemStatus status) {
            this.status = status;
            return this;
        }

        public Builder condition(ItemCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags == null ? new ArrayList<String>() : tags;
            return this;
        }

        public Builder favoriteCount(int favoriteCount) {
            this.favoriteCount = favoriteCount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Item build() {
            if (ownerId <= 0) {
                throw new ValidationException("物品必须属于某个发布者");
            }
            if (categoryId <= 0) {
                throw new ValidationException("必须选择物品分类");
            }
            if (title == null || title.trim().isEmpty()) {
                throw new ValidationException("物品标题不能为空");
            }
            String trimmedTitle = title.trim();
            if (trimmedTitle.length() > TITLE_MAX) {
                throw new ValidationException("物品标题不能超过 " + TITLE_MAX + " 个字符");
            }
            String desc = description == null ? "" : description.trim();
            if (desc.length() > DESCRIPTION_MAX) {
                throw new ValidationException("物品描述不能超过 " + DESCRIPTION_MAX + " 个字符");
            }
            if (price == null || price.signum() < 0) {
                throw new ValidationException("物品价格不能为负数");
            }
            if (price.scale() > 2) {
                throw new ValidationException("物品价格最多保留两位小数");
            }
            if (tradeType == null || status == null || condition == null) {
                throw new ValidationException("物品的流转方式、状态、成色不能为空");
            }
            this.title = trimmedTitle;
            this.description = desc;
            if (createdAt == null) {
                this.createdAt = LocalDateTime.now();
            }
            if (updatedAt == null) {
                this.updatedAt = this.createdAt;
            }
            return new Item(this);
        }
    }
}
