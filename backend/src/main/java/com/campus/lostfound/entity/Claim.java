package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认领申请
 */
@Data
@TableName("claim")
public class Claim {

    /** 等待发布者处理 */
    public static final String STATUS_PENDING = "PENDING";
    /** 发布者已同意 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 发布者已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 申请人主动撤销 */
    public static final String STATUS_CANCELED = "CANCELED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long itemId;

    private Long claimantId;

    /** 认领说明 / 物品特征凭证 */
    private String description;

    private String contact;

    private String status;

    /** 发布者处理时填写的备注 */
    private String auditRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
