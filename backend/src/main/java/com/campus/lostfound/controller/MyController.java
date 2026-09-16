package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.vo.ItemVO;
import com.campus.lostfound.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 个人中心数据（需要登录）
 */
@Tag(name = "04-个人中心", description = "我的发布与统计")
@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

    private final ItemService itemService;

    @Operation(summary = "我发布的信息（含各审核状态，status 传 ALL 查全部）")
    @GetMapping("/items")
    public Result<PageVO<ItemVO>> myItems(ItemQuery query) {
        return Result.ok(itemService.myItems(UserContext.requireUserId(), query));
    }

    @Operation(summary = "个人中心统计数字")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(itemService.myStats(UserContext.requireUserId()));
    }
}
