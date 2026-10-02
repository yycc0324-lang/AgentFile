package com.agentforge.platform.exception;

/**
 * 业务异常。
 * 业务代码里主动抛出，由 GlobalExceptionHandler 统一转成 Result。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(40000, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}