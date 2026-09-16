package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 信息审核请求（管理员）
 */
@Data
public class AuditDTO {

    @NotNull(message = "请选择审核结果")
    private Boolean approved;

    @Size(max = 100, message = "驳回原因最长 100 个字符")
    private String reason;
}
