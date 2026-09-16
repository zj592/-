package com.campus.lostfound.security;

import com.campus.lostfound.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 登录态 + 角色鉴权拦截器
 * <p>
 * 规则：
 * 1. /api/auth/** 不拦截（注册、登录）；
 * 2. 公开的 GET 接口（信息列表、详情、分类）可以匿名访问，但带着 token 时会解析出用户，
 * 便于返回"是否本人发布"这类标记；
 * 3. 其余接口必须携带有效 token，否则 401；
 * 4. 标注了 @RequireRole 的接口再做角色校验，不匹配返回 403。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * 允许匿名访问的 GET 接口
     */
    private static final Pattern PUBLIC_GET = Pattern.compile("^/api/items(/\\d+|/meta)?$");

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 1. 尝试解析 token（解析失败不直接抛，交给下面的规则决定）
        UserContext.LoginUser loginUser = resolveLoginUser(request);

        boolean publicGet = HttpMethod.GET.matches(request.getMethod())
                && PUBLIC_GET.matcher(request.getRequestURI()).matches();

        if (loginUser == null) {
            if (publicGet) {
                return true;
            }
            throw BusinessException.unauthorized("登录已失效，请重新登录");
        }

        UserContext.set(loginUser);

        // 2. 角色校验
        RequireRole requireRole = findRequireRole(handler);
        if (requireRole != null && !Arrays.asList(requireRole.value()).contains(loginUser.role())) {
            throw BusinessException.forbidden("当前账号无权访问该接口");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private UserContext.LoginUser resolveLoginUser(HttpServletRequest request) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Number uid = claims.get(JwtUtil.CLAIM_USER_ID, Number.class);
            if (uid == null) {
                return null;
            }
            String role = claims.get(JwtUtil.CLAIM_ROLE, String.class);
            return new UserContext.LoginUser(uid.longValue(), claims.getSubject(), role == null ? "STUDENT" : role);
        } catch (Exception e) {
            // token 过期 / 被篡改，按未登录处理
            return null;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        // 小程序端也可以直接用 token 头
        return request.getHeader("token");
    }

    private RequireRole findRequireRole(Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole onMethod = handlerMethod.getMethodAnnotation(RequireRole.class);
            if (onMethod != null) {
                return onMethod;
            }
            return handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        return null;
    }
}
