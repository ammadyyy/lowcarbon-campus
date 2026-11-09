package com.campus.lowcarbon.common;

/**
 * 业务异常基类：所有可被界面友好提示的异常都继承自该类。
 * <p>通过 {@link #getCode()} 区分异常种类，便于 GUI 层统一处理（迭代三）。</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
