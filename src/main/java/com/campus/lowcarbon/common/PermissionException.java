package com.campus.lowcarbon.common;

/** 越权操作，例如修改他人发布的物品。 */
public class PermissionException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public PermissionException(String message) {
        super("FORBIDDEN", message);
    }
}
