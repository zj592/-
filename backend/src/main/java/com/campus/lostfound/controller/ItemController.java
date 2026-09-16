package com.campus.lostfound.controller;

import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.ItemDTO;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.service.ItemService;
import com.campus.lostfound.vo.ItemVO;
import com.campus.lostfound.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 失物招领信息
 * 列表和详情可以匿名访问，发布/编辑/删除需要登录
 */
@Tag(name = "03-失物招领", description = "信息发布与检索")
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @Operation(summary = "首页列表（仅审核通过的信息，支持关键词/类型/分类筛选）")
    @GetMapping
    public Result<PageVO<ItemVO>> list(ItemQuery query) {
        return Result.ok(itemService.pageItems(query));
    }

    @Operation(summary = "字典数据：物品分类、信息类型")
    @GetMapping("/meta")
    public Result<Map<String, Object>> meta() {
        return Result.ok(itemService.meta());
    }

    @Operation(summary = "信息详情")
    @GetMapping("/{id}")
    public Result<ItemVO> detail(@PathVariable Long id) {
        return Result.ok(itemService.detail(id, UserContext.getOrNull() == null ? null : UserContext.get().id()));
    }

    @Operation(summary = "发布信息（需登录，发布后进入待审核）")
    @PostMapping
    public Result<ItemVO> publish(@Valid @RequestBody ItemDTO dto) {
        return Result.ok("发布成功，等待管理员审核", itemService.publish(UserContext.requireUserId(), dto));
    }

    @Operation(summary = "编辑信息（仅发布者，编辑后重新审核）")
    @PutMapping("/{id}")
    public Result<ItemVO> update(@PathVariable Long id, @Valid @RequestBody ItemDTO dto) {
        return Result.ok("修改成功，等待重新审核", itemService.update(id, UserContext.requireUserId(), dto));
    }

    @Operation(summary = "删除信息（仅发布者或管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        itemService.delete(id, UserContext.requireUserId());
        return Result.ok("已删除", null);
    }

    @Operation(summary = "标记为已完成（物归原主）")
    @PostMapping("/{id}/finish")
    public Result<Void> finish(@PathVariable Long id) {
        itemService.finish(id, UserContext.requireUserId());
        return Result.ok("已标记为完成", null);
    }
}
