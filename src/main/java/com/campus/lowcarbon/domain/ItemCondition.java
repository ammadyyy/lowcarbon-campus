package com.campus.lowcarbon.domain;

import com.campus.lowcarbon.common.ValidationException;

/** 物品成色。 */
public enum ItemCondition {

    NEW("NEW", "全新"),
    LIKE_NEW("LIKE_NEW", "九成新"),
    GOOD("GOOD", "七成新"),
    FAIR("FAIR", "有使用痕迹");

    private final String code;
    private final String label;

    ItemCondition(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static ItemCondition fromCode(String code) {
        for (ItemCondition condition : values()) {
            if (condition.code.equals(code)) {
                return condition;
            }
        }
        throw new ValidationException("非法的物品成色：" + code);
    }

    @Override
    public String toString() {
        return label;
    }
}
