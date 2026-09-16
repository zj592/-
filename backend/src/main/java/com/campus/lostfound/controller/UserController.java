package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人资料（需要登录）
 */
@Tag(name = "02-用户", description = "个人资料")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/profile")
    public Result<User> profile() {
        return Result.ok(authService.profile(UserContext.requireUserId()));
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<User> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        return Result.ok("保存成功", authService.updateProfile(UserContext.requireUserId(), dto));
    }
}
