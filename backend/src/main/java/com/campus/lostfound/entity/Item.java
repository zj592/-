package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失物 / 寻物信息
 */
@Data
@TableName("item")
public class Item {

    /** 寻物启事：我丢了东西 */
    public static final String TYPE_LOST = "LOST";
    /** 失物招领：我捡到了东西 */
    public static final String TYPE_FOUND = "FOUND";

    /** 待审核 */
    public static final String STATUS_PENDING = "PENDING";
    /** 审核通过，公开展示 */
    public static final String STATUS_APPROVED = "APPROVED";
    /** 已驳回 */
    public static final String STATUS_REJECTED = "REJECTED";
    /** 已完成（物归原主或已被认领） */
    public static final String STATUS_FINISHED = "FINISHED";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    /** LOST / FOUND */
    private String type;

    /** card / digital / device / key / other */
    private String category;

    private String description;

    /** 图片地址，多个用英文逗号分隔 */
    private String images;

    /** 丢失或拾取时间 */
    private LocalDateTime lostTime;

    /** 地点 */
    private String place;

    private String contact;

    private Long publisherId;

    private String status;

    private String rejectReason;

    private Integer viewCount;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
