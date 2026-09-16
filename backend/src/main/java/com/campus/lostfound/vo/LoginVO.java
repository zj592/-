package com.campus.lostfound.vo;

import lombok.Data;

/**
 * 登录返回
 */
@Data
public class LoginVO {

    private String token;

    private Long userId;

    private String username;

    private String nickname;

    private String role;

    private String avatar;

    /** token 有效秒数，小程序端用来判断是否要重新登录 */
    private Long expire;
}
