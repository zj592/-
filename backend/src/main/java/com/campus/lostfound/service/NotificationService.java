package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.entity.Notification;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.NotificationMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.vo.PageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 站内通知 + 微信订阅消息
 * <p>
 * 站内信是默认通道，永远可用；
 * 订阅消息需要公众平台申请模板并把 app.subscribe.enabled 打开，
 * 未配置时只打日志，绝不影响主业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;

    @Value("${app.subscribe.enabled:false}")
    private boolean subscribeEnabled;
    @Value("${app.subscribe.template.claim-result:}")
    private String tplClaimResult;
    @Value("${app.subscribe.template.item-audit:}")
    private String tplItemAudit;
    @Value("${app.subscribe.template.claim-received:}")
    private String tplClaimReceived;
    @Value("${app.wx.appid:}")
    private String appId;
    @Value("${app.wx.secret:}")
    private String appSecret;

    private final AtomicReference<AccessToken> tokenCache = new AtomicReference<>();

    /**
     * 写入站内信，并尝试推订阅消息
     */
    public void create(Long userId, String type, String title, String content, Long relatedId) {
        if (userId == null) {
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notificationMapper.insert(notification);

        // 站内信跟着业务事务提交；订阅消息属于外部 IO，放到事务提交后再发，避免长时间占用数据库连接
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            Long uid = userId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendSubscribeMessage(uid, type, title, content);
                }
            });
        } else {
            sendSubscribeMessage(userId, type, title, content);
        }
    }

    public PageVO<Notification> list(Long userId, int page, int size) {
        int current = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), 50);
        Page<Notification> result = notificationMapper.selectPage(
                new Page<>(current, pageSize),
                Wrappers.<Notification>lambdaQuery()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getId));
        return PageVO.of(result.getRecords(), result.getTotal(), current, pageSize);
    }

    public long unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return count == null ? 0 : count;
    }

    public void markRead(Long id, Long userId) {
        Notification notification = notificationMapper.selectById(id);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw BusinessException.notFound("通知不存在");
        }
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getId, id)
                .set(Notification::getIsRead, 1));
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    // ------------------------------------------------------------ 订阅消息

    private void sendSubscribeMessage(Long userId, String type, String title, String content) {
        String templateId = templateIdOf(type);
        if (!subscribeEnabled || !StringUtils.hasText(templateId)) {
            log.debug("订阅消息未启用或未配置模板（type={}），已降级为站内信", type);
            return;
        }
        try {
            User user = userMapper.selectById(userId);
            if (user == null || !StringUtils.hasText(user.getOpenid())) {
                // 账号密码注册的用户拿不到 openid，无法推送
                log.debug("用户 {} 没有 openid，跳过订阅消息", userId);
                return;
            }
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("thing1", Map.of("value", truncate(title, 20)));
            data.put("thing2", Map.of("value", truncate(content, 20)));
            data.put("time3", Map.of("value", LocalDateTime.now().format(TIME_FMT)));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("touser", user.getOpenid());
            body.put("template_id", templateId);
            body.put("page", pageOf(type));
            body.put("data", data);

            Map<String, Object> resp = RestClient.create()
                    .post()
                    .uri(SEND_URL + "?access_token={token}", accessToken())
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (resp != null && resp.get("errcode") != null && !"0".equals(String.valueOf(resp.get("errcode")))) {
                log.warn("订阅消息发送失败：{}", resp);
            }
        } catch (Exception e) {
            // 推送失败不能影响主业务
            log.warn("订阅消息发送异常，已忽略：{}", e.getMessage());
        }
    }

    private String accessToken() {
        AccessToken cached = tokenCache.get();
        if (cached != null && cached.expireAt() > System.currentTimeMillis()) {
            return cached.value();
        }
        Map<String, Object> resp = RestClient.create()
                .get()
                .uri(TOKEN_URL, appId, appSecret)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (resp == null || resp.get("access_token") == null) {
            throw new IllegalStateException("获取 access_token 失败：" + resp);
        }
        String token = String.valueOf(resp.get("access_token"));
        long expiresIn = resp.get("expires_in") == null ? 7200L : Long.parseLong(String.valueOf(resp.get("expires_in")));
        // 提前 5 分钟过期，避免临界点用到失效 token
        tokenCache.set(new AccessToken(token, System.currentTimeMillis() + (expiresIn - 300) * 1000L));
        return token;
    }

    private String templateIdOf(String type) {
        return switch (type) {
            case Notification.TYPE_CLAIM_RESULT -> tplClaimResult;
            case Notification.TYPE_ITEM_AUDIT -> tplItemAudit;
            case Notification.TYPE_CLAIM_RECEIVED -> tplClaimReceived;
            default -> "";
        };
    }

    private String pageOf(String type) {
        return switch (type) {
            case Notification.TYPE_CLAIM_RESULT -> "pages/my-claims/my-claims";
            default -> "pages/my-items/my-items";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private record AccessToken(String value, long expireAt) {
    }

    /**
     * 供前端授权后回执，当前仅记录日志（授权结果由前端持有，推送时由平台校验）
     */
    public List<String> supportedTypes() {
        return List.of(Notification.TYPE_CLAIM_RECEIVED, Notification.TYPE_CLAIM_RESULT, Notification.TYPE_ITEM_AUDIT);
    }
}
