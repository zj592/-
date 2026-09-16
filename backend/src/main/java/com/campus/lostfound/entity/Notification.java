package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知
 */
@Data
@TableName("notification")
public class Notification {

    /** 收到新的认领申请（发给发布者） */
    public static final String TYPE_CLAIM_RECEIVED = "CLAIM_RECEIVED";
    /** 认领结果（发给申请人） */
    public static final String TYPE_CLAIM_RESULT = "CLAIM_RESULT";
    /** 信息审核结果（发给发布者） */
    public static final String TYPE_ITEM_AUDIT = "ITEM_AUDIT";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人 */
    private Long userId;

    private String type;

    private String title;

    private String content;

    /** 关联业务 id：信息 id 或申请 id */
    private Long relatedId;

    /** 0 未读 / 1 已读 */
    private Integer isRead;

    private LocalDateTime createTime;
}
