package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交认领申请
 */
@Data
public class ClaimDTO {

    @NotNull(message = "信息 id 不能为空")
    private Long itemId;

    /**
     * 认领说明：描述物品特征，用来证明东西是你的
     */
    @Size(min = 5, max = 200, message = "认领说明长度需在 5-200 个字符之间")
    private String description;

    @Size(max = 60, message = "联系方式最长 60 个字符")
    private String contact;
}
