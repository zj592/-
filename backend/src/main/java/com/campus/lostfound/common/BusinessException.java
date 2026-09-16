package com.campus.lostfound.common;

import lombok.Getter;

/**
 * 业务异常，由 GlobalExceptionHandler 统一转成 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(Result.CODE_ERROR, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(Result.CODE_BAD_REQUEST, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(Result.CODE_UNAUTHORIZED, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(Result.CODE_FORBIDDEN, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(Result.CODE_NOT_FOUND, message);
    }
}
