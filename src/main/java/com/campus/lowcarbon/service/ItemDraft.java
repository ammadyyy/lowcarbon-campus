package com.campus.lowcarbon.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.campus.lowcarbon.common.ValidationException;
import com.campus.lowcarbon.common.Validators;
import com.campus.lowcarbon.domain.Item;
import com.campus.lowcarbon.domain.ItemCondition;
import com.campus.lowcarbon.domain.TradeType;

/**
 * 发布 / 修改物品时的输入对象（DTO + Builder）。
 * <p>
 * Service 层不直接接收 {@link Item} 实体，避免调用方伪造 id、状态、收藏数等
 * 本应由系统维护的字段。所有字段在这里完成格式校验，再组装成实体。
 * </p>
 */
public final class ItemDraft {

    private final long categoryId;
    private final String title;
    private final String description;
    private final BigDecimal price;
    private final TradeType tradeType;
    private final ItemCondition condition;
    private final List<String> tags;

    private ItemDraft(Builder builder) {
        this.categoryId = builder.categoryId;
        this.title = builder.title;
        this.description = builder.description;
        this.price = builder.price;
        this.tradeType = builder.tradeType;
        this.condition = builder.condition;
        this.tags = Collections.unmodifiableList(new ArrayList<String>(builder.tags));
    }

    public static Builder builder() {
        return new Builder();
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

    public ItemCondition getCondition() {
        return condition;
    }

    public List<String> getTags() {
        return tags;
    }

    /** 发布信息构造器：在此统一做格式校验。 */
    public static final class Builder {

        private long categoryId;
        private String title;
        private String description = "";
        private BigDecimal price = BigDecimal.ZERO;
        private TradeType tradeType = TradeType.SELL;
        private ItemCondition condition = ItemCondition.GOOD;
        private List<String> tags = new ArrayList<String>();

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

        public Builder price(String priceText) {
            this.price = Validators.parsePrice(priceText);
            return this;
        }

        public Builder tradeType(TradeType tradeType) {
            this.tradeType = tradeType;
            return this;
        }

        public Builder condition(ItemCondition condition) {
            this.condition = condition;
            return this;
        }

        /** 接收 "教材,九成新" 形式的原始标签串，交由 Validators 规范化。 */
        public Builder tags(String rawTags) {
            this.tags = Validators.splitTags(Validators.normalizeTags(rawTags));
            return this;
        }

        public Builder tags(List<String> tags) {
            if (tags == null) {
                this.tags = new ArrayList<String>();
            } else {
                StringBuilder raw = new StringBuilder();
                for (String tag : tags) {
                    if (raw.length() > 0) {
                        raw.append(Validators.TAG_SEPARATOR);
                    }
                    raw.append(tag);
                }
                return tags(raw.toString());
            }
            return this;
        }

        public ItemDraft build() {
            if (categoryId <= 0) {
                throw new ValidationException("必须选择物品分类");
            }
            String safeTitle = Validators.requireText(title, "物品标题", 1, Item.TITLE_MAX);
            String safeDescription = Validators.optionalText(description, "物品描述", Item.DESCRIPTION_MAX);
            BigDecimal safePrice = Validators.requirePrice(price == null ? BigDecimal.ZERO : price);
            if (tradeType == null) {
                throw new ValidationException("请选择流转方式");
            }
            if (condition == null) {
                throw new ValidationException("请选择物品成色");
            }
            this.title = safeTitle;
            this.description = safeDescription;
            this.price = safePrice;
            return new ItemDraft(this);
        }
    }
}
