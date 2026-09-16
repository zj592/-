package com.campus.lostfound.security;

import com.campus.lostfound.common.BusinessException;

/**
 * 当前登录用户上下文（ThreadLocal）
 * 由 AuthInterceptor 写入，请求结束后清除
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /**
     * 取当前用户 id，未登录抛 401
     */
    public static Long requireUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        return user.id();
    }

    /**
     * 取当前用户，允许为空（公开接口也要判断"是不是我发的"时用）
     */
    public static LoginUser getOrNull() {
        return HOLDER.get();
    }

    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "ADMIN".equals(user.role());
    }

    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 登录用户信息
     */
    public record LoginUser(Long id, String username, String role) {
    }
}
