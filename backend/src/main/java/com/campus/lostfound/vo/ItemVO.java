package com.campus.lostfound.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信息列表 / 详情返回结构
 */
@Data
public class ItemVO {

    private Long id;

    private String title;

    private String type;

    private String typeText;

    private String category;

    private String categoryText;

    private String description;

    private List<String> images;

    /** 列表页封面图 */
    private String cover;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lostTime;

    private String place;

    /** 联系方式：未登录时打码，登录后可见完整 */
    private String contact;

    private String status;

    private String statusText;

    private String rejectReason;

    private Integer viewCount;

    private Long publisherId;

    private String publisherNickname;

    private String publisherAvatar;

    /** 当前请求者是否为发布者 */
    private Boolean owner;

    /** 当前请求者是否已提交过认领申请（未登录为 null） */
    private Boolean claimed;

    /** 详情页才有：该信息收到的认领申请数 */
    private Integer claimCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
