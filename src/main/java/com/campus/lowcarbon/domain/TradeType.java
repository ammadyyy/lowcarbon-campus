package com.campus.lowcarbon.domain;

import com.campus.lowcarbon.common.ValidationException;

/** 流转方式：售卖、交换、赠送。 */
public enum TradeType {

    SELL("SELL", "售卖"),
    EXCHANGE("EXCHANGE", "交换"),
    DONATE("DONATE", "赠送");

    private final String code;
    private final String label;

    TradeType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static TradeType fromCode(String code) {
        for (TradeType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new ValidationException("非法的流转方式：" + code);
    }

    @Override
    public String toString() {
        return label;
    }
}
