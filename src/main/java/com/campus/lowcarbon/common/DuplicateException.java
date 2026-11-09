package com.campus.lowcarbon.common;

/** 唯一性约束冲突，例如用户名、分类名重复。 */
public class DuplicateException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public DuplicateException(String message) {
        super("DUPLICATE", message);
    }
}
