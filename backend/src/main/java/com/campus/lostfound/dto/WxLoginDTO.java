package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录
 */
@Data
public class WxLoginDTO {

    /** wx.login 拿到的临时凭证 */
    @NotBlank(message = "code 不能为空")
    private String code;

    /**
     * 小程序端持久化的设备标识。
     * 只有在未配置 appid/secret 的 mock 模式下才会用到，用来保证同一台设备反复登录仍然是同一个账号。
     */
    private String deviceId;

    /** 可选：用户授权后带过来的昵称 */
    private String nickname;

    private String avatar;
}
