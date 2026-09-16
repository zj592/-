package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 认领申请返回结构
 */
@Data
public class ClaimVO {

    private Long id;

    private Long itemId;

    private String itemTitle;

    private String itemType;

    private String itemTypeText;

    private String itemCover;

    /** 信息当前状态，发布者处理申请时需要参考 */
    private String itemStatus;

    private Long claimantId;

    private String claimantNickname;

    private String description;

    private String contact;

    private String status;

    private String statusText;

    private String auditRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
