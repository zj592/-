package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.security.RequireRole;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.ItemVO;
import com.campus.lostfound.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台管理：整类接口限管理员访问（@RequireRole 由拦截器校验）
 */
@Tag(name = "06-后台管理", description = "信息审核与数据概览（仅管理员）")
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminController {

    private final ItemService itemService;
    private final ClaimService claimService;

    @Operation(summary = "待审核信息列表（status 不传默认查待审核，传 ALL 查全部）")
    @GetMapping("/items")
    public Result<PageVO<ItemVO>> items(ItemQuery query) {
        return Result.ok(itemService.auditPage(query));
    }

    @Operation(summary = "审核信息：通过 / 驳回")
    @PostMapping("/items/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        itemService.audit(id, dto);
        return Result.ok(Boolean.TRUE.equals(dto.getApproved()) ? "已通过审核" : "已驳回", null);
    }

    @Operation(summary = "全部认领申请")
    @GetMapping("/claims")
    public Result<PageVO<ClaimVO>> claims(@RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(claimService.allClaims(status, page, size));
    }

    @Operation(summary = "数据概览")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(itemService.stats());
    }
}
