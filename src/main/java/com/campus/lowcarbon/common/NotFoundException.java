package com.campus.lowcarbon.common;

/** 查询的数据不存在。 */
public class NotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
