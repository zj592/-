package com.campus.lostfound.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户
 */
@Data
@TableName("user")
public class User {

    /** 角色：学生 */
    public static final String ROLE_STUDENT = "STUDENT";
    /** 角色：管理员 */
    public static final String ROLE_ADMIN = "ADMIN";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 密文，永远不返回给前端 */
    @JsonIgnore
    private String password;

    /** 微信小程序 openid，账号密码注册的用户为空 */
    private String openid;

    private String nickname;

    private String phone;

    private String campus;

    private String avatar;

    private String role;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
