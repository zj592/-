package com.campus.lostfound.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布 / 编辑 失物招领信息
 */
@Data
public class ItemDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 50, message = "标题最长 50 个字符")
    private String title;

    @NotBlank(message = "请选择信息类型")
    @Pattern(regexp = "^(LOST|FOUND)$", message = "信息类型只能是 LOST 或 FOUND")
    private String type;

    @NotBlank(message = "请选择物品分类")
    @Pattern(regexp = "^(card|digital|device|key|book|other)$", message = "物品分类不合法")
    private String category;

    @NotBlank(message = "请填写详细描述")
    @Size(min = 5, max = 500, message = "描述长度需在 5-500 个字符之间")
    private String description;

    /** 图片地址列表，最多 3 张 */
    @Size(max = 3, message = "最多上传 3 张图片")
    private List<String> images;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lostTime;

    @Size(max = 60, message = "地点最长 60 个字符")
    private String place;

    @NotBlank(message = "请填写联系方式")
    @Size(max = 60, message = "联系方式最长 60 个字符")
    private String contact;
}
