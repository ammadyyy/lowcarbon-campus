package com.campus.lowcarbon.domain;

import com.campus.lowcarbon.common.ValidationException;

/**
 * 物品分类实体。分类由管理员维护，被禁用后不再出现在发布/筛选下拉框中。
 */
public final class Category {

    private final long id;
    private final String name;
    private final String description;
    private final int sortOrder;
    private final boolean enabled;

    private Category(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.sortOrder = builder.sortOrder;
        this.enabled = builder.enabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name) {
        return new Builder().name(name);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .enabled(enabled);
    }

    @Override
    public String toString() {
        return name;
    }

    /** 分类构造器，统一做输入校验。 */
    public static final class Builder {

        private long id;
        private String name;
        private String description = "";
        private int sortOrder = 0;
        private boolean enabled = true;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder sortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Category build() {
            if (name == null || name.trim().isEmpty()) {
                throw new ValidationException("分类名称不能为空");
            }
            if (name.trim().length() > 20) {
                throw new ValidationException("分类名称不能超过 20 个字符");
            }
            return new Category(this);
        }
    }
}
