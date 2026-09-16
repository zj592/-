package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.entity.Notification;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.service.NotificationService;
import com.campus.lostfound.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息通知（需要登录）
 */
@Tag(name = "08-消息通知", description = "站内信与未读数")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "我的消息列表")
    @GetMapping
    public Result<PageVO<Notification>> list(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(notificationService.list(UserContext.requireUserId(), page, size));
    }

    @Operation(summary = "未读消息数")
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", notificationService.unreadCount(UserContext.requireUserId()));
        return Result.ok(data);
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id, UserContext.requireUserId());
        return Result.ok("已读", null);
    }

    @Operation(summary = "全部标记已读")
    @PostMapping("/read-all")
    public Result<Void> readAll() {
        notificationService.markAllRead(UserContext.requireUserId());
        return Result.ok("已全部标记已读", null);
    }
}
