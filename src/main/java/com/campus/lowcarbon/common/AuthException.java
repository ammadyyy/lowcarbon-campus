package com.campus.lowcarbon.common;

/** 认证失败：账号不存在、密码错误、账号被封禁。 */
public class AuthException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AuthException(String message) {
        super("AUTH", message);
    }
}
