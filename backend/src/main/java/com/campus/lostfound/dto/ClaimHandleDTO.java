package com.campus.lostfound.dto;

import lombok.Data;

/**
 * 发布者处理认领申请
 */
@Data
public class ClaimHandleDTO {

    /** true 同意认领 / false 驳回 */
    private Boolean approved;

    private String remark;
}
