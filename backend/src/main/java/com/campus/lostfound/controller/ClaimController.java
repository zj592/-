package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.ClaimDTO;
import com.campus.lostfound.dto.ClaimHandleDTO;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.vo.ClaimVO;
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

/**
 * 认领申请（需要登录）
 */
@Tag(name = "05-认领申请", description = "申请认领 / 处理申请")
@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @Operation(summary = "提交认领申请")
    @PostMapping
    public Result<ClaimVO> submit(@Valid @RequestBody ClaimDTO dto) {
        return Result.ok("已提交申请，等待发布者确认", claimService.submit(UserContext.requireUserId(), dto));
    }

    @Operation(summary = "我提交的认领申请")
    @GetMapping("/mine")
    public Result<PageVO<ClaimVO>> mine(@RequestParam(required = false) String status,
                                       @RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(claimService.myClaims(UserContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "我发布的信息收到的申请")
    @GetMapping("/received")
    public Result<PageVO<ClaimVO>> received(@RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size) {
        return Result.ok(claimService.receivedClaims(UserContext.requireUserId(), status, page, size));
    }

    @Operation(summary = "撤销申请（仅待处理状态）")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        claimService.cancel(id, UserContext.requireUserId());
        return Result.ok("已撤销", null);
    }

    @Operation(summary = "发布者处理申请：同意 / 驳回")
    @PostMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody ClaimHandleDTO dto) {
        if (dto.getApproved() == null) {
            return Result.fail(400, "请选择处理结果");
        }
        claimService.handle(id, UserContext.requireUserId(), dto.getApproved(), dto.getRemark());
        return Result.ok(dto.getApproved() ? "已确认认领，信息标记为完成" : "已驳回该申请", null);
    }
}
