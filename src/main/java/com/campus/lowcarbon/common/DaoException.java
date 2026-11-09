package com.campus.lowcarbon.common;

/** 数据访问层异常：把受检的 SQLException 统一包装为运行时异常，避免污染上层签名。 */
public class DaoException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public DaoException(String message) {
        super("DAO", message);
    }

    public DaoException(String message, Throwable cause) {
        super("DAO", message, cause);
    }
}
