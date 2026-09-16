package com.campus.lostfound.dto;

import lombok.Data;

/**
 * 信息列表查询条件（列表检索 / 我的发布 / 后台审核共用）
 */
@Data
public class ItemQuery {

    /** 关键词：标题、描述、地点模糊匹配 */
    private String keyword;

    /** LOST / FOUND，空表示全部 */
    private String type;

    /** 物品分类 */
    private String category;

    /** 状态：不传时列表默认只查 APPROVED */
    private String status;

    /** 发布人 id，后台/我的发布用 */
    private Long publisherId;

    /** 排序：latest-最新发布 view-最多浏览 */
    private String sort = "latest";

    private Integer page = 1;

    private Integer size = 10;

    public long currentPage() {
        return page == null || page < 1 ? 1L : page;
    }

    public long pageSize() {
        if (size == null || size < 1) {
            return 10L;
        }
        return Math.min(size, 50);
    }
}
