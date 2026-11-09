package com.campus.lowcarbon.common;

/** 输入校验失败（防御性编程的统一出口）。 */
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super("VALIDATION", message);
    }
}
