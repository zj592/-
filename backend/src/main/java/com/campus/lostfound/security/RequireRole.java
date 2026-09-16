package com.campus.lostfound.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级角色校验，配合 AuthInterceptor 使用。
 * 例：@RequireRole("ADMIN") 表示仅管理员可访问。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 允许访问的角色，任意一个匹配即放行
     */
    String[] value() default {"ADMIN"};
}
