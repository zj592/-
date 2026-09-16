package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.dto.WxLoginDTO;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.JwtUtil;
import com.campus.lostfound.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;

/**
 * 注册 / 登录（含微信登录）/ 个人资料
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.wx.appid:}")
    private String wxAppId;
    @Value("${app.wx.secret:}")
    private String wxSecret;
    @Value("${app.wx.mock:true}")
    private boolean wxMock;
    @Value("${app.wx.jscode2session-url:https://api.weixin.qq.com/sns/jscode2session}")
    private String jscode2sessionUrl;

    /**
     * 注册成功后直接返回 token，省掉一次登录请求
     */
    public LoginVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw BusinessException.badRequest("该账号已被注册");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setCampus(dto.getCampus());
        user.setRole(User.ROLE_STUDENT);
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        // 账号不存在和密码错误返回同一句提示，避免被人枚举账号
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw BusinessException.forbidden("该账号已被禁用");
        }
        return buildLoginVO(user);
    }

    /**
     * 微信小程序登录：code 换 openid，首次登录自动建号
     */
    public LoginVO wxLogin(WxLoginDTO dto) {
        String openid = resolveOpenid(dto);
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));

        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setUsername(uniqueUsername(openid));
            // 微信登录用户没有密码，写一个随机密文占位，保证 password 非空
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname(StringUtils.hasText(dto.getNickname())
                    ? dto.getNickname()
                    : "微信用户" + suffix(openid));
            user.setAvatar(dto.getAvatar());
            user.setRole(User.ROLE_STUDENT);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("微信用户首次登录，自动创建账号：id={} username={}", user.getId(), user.getUsername());
        } else {
            if (user.getStatus() == null || user.getStatus() != 1) {
                throw BusinessException.forbidden("该账号已被禁用");
            }
            // 用户可能改了微信昵称/头像，这边同步一下
            if (StringUtils.hasText(dto.getNickname()) || StringUtils.hasText(dto.getAvatar())) {
                userMapper.update(null, Wrappers.<User>lambdaUpdate()
                        .eq(User::getId, user.getId())
                        .set(StringUtils.hasText(dto.getNickname()), User::getNickname, dto.getNickname())
                        .set(StringUtils.hasText(dto.getAvatar()), User::getAvatar, dto.getAvatar()));
                user = profile(user.getId());
            }
        }
        return buildLoginVO(user);
    }

    public User profile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    public User updateProfile(Long userId, ProfileDTO dto) {
        // 只更新传进来的字段，避免小程序端只改昵称时把手机号、校区清空
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getNickname, dto.getNickname())
                .set(dto.getPhone() != null, User::getPhone, dto.getPhone())
                .set(dto.getCampus() != null, User::getCampus, dto.getCampus())
                .set(dto.getAvatar() != null && !dto.getAvatar().isBlank(), User::getAvatar, dto.getAvatar()));
        return profile(userId);
    }

    // ------------------------------------------------------------ 微信登录内部实现

    /**
     * 未配置 appid/secret（或显式开启 mock）时，用小程序端持久化的 deviceId 派生一个稳定的假 openid，
     * 保证开发者工具 / 测试号也能把登录链路跑通。
     */
    private String resolveOpenid(WxLoginDTO dto) {
        if (wxMock || !StringUtils.hasText(wxAppId) || !StringUtils.hasText(wxSecret)) {
            String seed = StringUtils.hasText(dto.getDeviceId()) ? dto.getDeviceId() : dto.getCode();
            log.info("微信登录运行在 mock 模式（未配置 appid/secret），使用本地标识派生 openid");
            return "mock_" + sha256Hex(seed).substring(0, 24);
        }

        Map<String, Object> resp;
        try {
            resp = RestClient.create()
                    .get()
                    .uri(jscode2sessionUrl
                                    + "?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                            wxAppId, wxSecret, dto.getCode())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (Exception e) {
            log.error("调用 jscode2session 失败", e);
            throw BusinessException.badRequest("微信登录失败，请稍后重试");
        }
        if (resp == null || resp.get("openid") == null) {
            String errmsg = resp == null ? "无响应" : String.valueOf(resp.get("errmsg"));
            log.warn("微信登录返回异常：{}", resp);
            throw BusinessException.badRequest("微信登录失败：" + errmsg);
        }
        return String.valueOf(resp.get("openid"));
    }

    /**
     * 由 openid 派生用户名，冲突时补随机后缀
     */
    private String uniqueUsername(String openid) {
        String base = "wx_" + Math.abs(openid.hashCode() % 100000000);
        String username = base;
        while (userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username)) > 0) {
            username = base + "_" + UUID.randomUUID().toString().substring(0, 4);
        }
        return username;
    }

    private String suffix(String openid) {
        return openid.length() <= 4 ? openid : openid.substring(openid.length() - 4);
    }

    private String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(Math.abs(text.hashCode()));
        }
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generate(user.getId(), user.getUsername(), user.getRole()));
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        vo.setAvatar(user.getAvatar());
        vo.setExpire(jwtUtil.getExpireSeconds());
        return vo;
    }
}

