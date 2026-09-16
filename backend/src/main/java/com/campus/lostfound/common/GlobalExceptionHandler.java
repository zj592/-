package com.campus.lostfound.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：所有异常统一转成 Result，避免把堆栈直接抛给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * @Valid 校验失败（@RequestBody 场景）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        return Result.fail(Result.CODE_BAD_REQUEST, firstError(e.getBindingResult().getFieldError()));
    }

    /**
     * @Valid 校验失败（表单/查询参数场景）
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.fail(Result.CODE_BAD_REQUEST, firstError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.fail(Result.CODE_ERROR, "系统繁忙，请稍后再试");
    }

    private String firstError(FieldError error) {
        if (error == null) {
            return "参数校验失败";
        }
        return error.getDefaultMessage();
    }
}
