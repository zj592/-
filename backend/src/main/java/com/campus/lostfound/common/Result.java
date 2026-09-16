package com.campus.lostfound.common;

import lombok.Data;

/**
 * 统一响应体
 */
@Data
public class Result<T> {

    public static final int CODE_OK = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_ERROR = 500;

    private int code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_OK, "操作成功", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_OK, "操作成功", data);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(CODE_OK, msg, data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(CODE_ERROR, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
