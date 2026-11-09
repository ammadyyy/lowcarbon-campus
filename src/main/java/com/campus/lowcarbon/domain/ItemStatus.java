package com.campus.lowcarbon.domain;

import com.campus.lowcarbon.common.ValidationException;

/**
 * 物品状态机。
 * <pre>
 * ON_SALE  --下架-->  OFF_SHELF  --重新上架-->  ON_SALE
 * ON_SALE  --被下单--> LOCKED     --成交-->     COMPLETED
 * LOCKED   --取消-->   ON_SALE
 * COMPLETED（终态，不可再变更）
 * </pre>
 */
public enum ItemStatus {

    ON_SALE("ON_SALE", "在架"),
    LOCKED("LOCKED", "交易中"),
    OFF_SHELF("OFF_SHELF", "已下架"),
    COMPLETED("COMPLETED", "已完成");

    private final String code;
    private final String label;

    ItemStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    /** 是否允许从当前状态迁移到目标状态。 */
    public boolean canTransitionTo(ItemStatus target) {
        if (target == null || target == this) {
            return false;
        }
        switch (this) {
            case ON_SALE:
                return target == OFF_SHELF || target == LOCKED;
            case LOCKED:
                return target == COMPLETED || target == ON_SALE;
            case OFF_SHELF:
                return target == ON_SALE;
            default:
                return false;
        }
    }

    /** 是否为可被检索到的在架物品。 */
    public boolean isVisible() {
        return this == ON_SALE || this == LOCKED;
    }

    public static ItemStatus fromCode(String code) {
        for (ItemStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new ValidationException("非法的物品状态：" + code);
    }

    @Override
    public String toString() {
        return label;
    }
}
