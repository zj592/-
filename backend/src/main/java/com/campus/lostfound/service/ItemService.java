package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.Labels;
import com.campus.lostfound.dto.AuditDTO;
import com.campus.lostfound.dto.ItemDTO;
import com.campus.lostfound.dto.ItemQuery;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.Notification;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.vo.ItemVO;
import com.campus.lostfound.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 失物招领信息：发布、检索、详情、审核、统计
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final ClaimMapper claimMapper;
    private final NotificationService notificationService;

    // ------------------------------------------------------------ 公共接口

    /**
     * 小程序端字典：分类、类型
     */
    public Map<String, Object> meta() {
        return Labels.all();
    }

    /**
     * 公开列表：只返回审核通过的信息
     */
    public PageVO<ItemVO> pageItems(ItemQuery query) {
        return page(query, null);
    }

    /**
     * 详情
     */
    public ItemVO detail(Long id, Long viewerId) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw BusinessException.notFound("信息不存在或已被删除");
        }
        boolean owner = viewerId != null && viewerId.equals(item.getPublisherId());
        boolean admin = UserContext.isAdmin();
        if (!Item.STATUS_APPROVED.equals(item.getStatus()) && !owner && !admin) {
            throw BusinessException.forbidden("该信息尚未通过审核");
        }

        // 浏览量自增，自己看自己的不计数
        if (!owner) {
            itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                    .eq(Item::getId, id)
                    .setSql("view_count = view_count + 1"));
            item.setViewCount((item.getViewCount() == null ? 0 : item.getViewCount()) + 1);
        }

        Map<Long, User> publishers = loadUsers(Collections.singletonList(item.getPublisherId()));
        ItemVO vo = toVO(item, publishers, viewerId, true);
        // 认领申请数只有发布者和管理员能看到
        if (owner || admin) {
            Long count = claimMapper.selectCount(Wrappers.<Claim>lambdaQuery()
                    .eq(Claim::getItemId, id)
                    .eq(Claim::getStatus, Claim.STATUS_PENDING));
            vo.setClaimCount(count == null ? 0 : count.intValue());
        }
        // 未登录时前端按钮走登录引导，登录后才知道是否已经申请过
        if (viewerId != null) {
            Long mine = claimMapper.selectCount(Wrappers.<Claim>lambdaQuery()
                    .eq(Claim::getItemId, id)
                    .eq(Claim::getClaimantId, viewerId)
                    .ne(Claim::getStatus, Claim.STATUS_CANCELED));
            vo.setClaimed(mine != null && mine > 0);
        }
        return vo;
    }

    // ------------------------------------------------------------ 登录用户接口

    /**
     * 发布信息，进入待审核状态
     */
    public ItemVO publish(Long userId, ItemDTO dto) {
        Item item = new Item();
        applyDTO(item, dto);
        item.setPublisherId(userId);
        item.setStatus(Item.STATUS_PENDING);
        item.setViewCount(0);
        itemMapper.insert(item);
        return toVO(item, loadUsers(Collections.singletonList(userId)), userId, true);
    }

    /**
     * 编辑信息：仅发布者本人或管理员；编辑后需要重新审核
     */
    public ItemVO update(Long id, Long userId, ItemDTO dto) {
        Item item = requireItem(id);
        checkOwnerOrAdmin(item, userId);
        applyDTO(item, dto);
        if (Item.STATUS_APPROVED.equals(item.getStatus())) {
            item.setStatus(Item.STATUS_PENDING);
        }
        item.setRejectReason(null);
        itemMapper.updateById(item);
        return toVO(item, loadUsers(Collections.singletonList(item.getPublisherId())), userId, true);
    }

    /**
     * 删除信息（逻辑删除）
     */
    public void delete(Long id, Long userId) {
        Item item = requireItem(id);
        checkOwnerOrAdmin(item, userId);
        itemMapper.deleteById(id);
    }

    /**
     * 发布者标记"已完成"（东西已找回归还）
     */
    public void finish(Long id, Long userId) {
        Item item = requireItem(id);
        if (!item.getPublisherId().equals(userId)) {
            throw BusinessException.forbidden("只有发布者可以操作");
        }
        if (Item.STATUS_FINISHED.equals(item.getStatus())) {
            throw BusinessException.badRequest("该信息已经是完成状态");
        }
        itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                .eq(Item::getId, id)
                .set(Item::getStatus, Item.STATUS_FINISHED));
    }

    /**
     * 我的发布：包含各种审核状态
     */
    public PageVO<ItemVO> myItems(Long userId, ItemQuery query) {
        query.setPublisherId(userId);
        if (!StringUtils.hasText(query.getStatus())) {
            query.setStatus("ALL");
        }
        return page(query, userId);
    }

    /**
     * 个人中心的统计数字
     */
    public Map<String, Object> myStats(Long userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("published", count(Wrappers.<Item>lambdaQuery().eq(Item::getPublisherId, userId)));
        map.put("pending", count(Wrappers.<Item>lambdaQuery()
                .eq(Item::getPublisherId, userId)
                .eq(Item::getStatus, Item.STATUS_PENDING)));
        map.put("finished", count(Wrappers.<Item>lambdaQuery()
                .eq(Item::getPublisherId, userId)
                .eq(Item::getStatus, Item.STATUS_FINISHED)));
        map.put("claims", countClaim(Wrappers.<Claim>lambdaQuery().eq(Claim::getClaimantId, userId)));
        // 别人申请认领我的信息，待我处理的数量
        List<Long> myItemIds = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                        .select(Item::getId)
                        .eq(Item::getPublisherId, userId))
                .stream().map(Item::getId).toList();
        long pendingReceived = 0;
        if (!myItemIds.isEmpty()) {
            pendingReceived = countClaim(Wrappers.<Claim>lambdaQuery()
                    .in(Claim::getItemId, myItemIds)
                    .eq(Claim::getStatus, Claim.STATUS_PENDING));
        }
        map.put("pendingReceived", pendingReceived);
        return map;
    }

    // ------------------------------------------------------------ 管理员接口

    /**
     * 后台审核列表
     */
    public PageVO<ItemVO> auditPage(ItemQuery query) {
        if (!StringUtils.hasText(query.getStatus())) {
            query.setStatus(Item.STATUS_PENDING);
        }
        return page(query, null);
    }

    /**
     * 审核：通过 / 驳回
     */
    public void audit(Long id, AuditDTO dto) {
        Item item = requireItem(id);
        if (Boolean.TRUE.equals(dto.getApproved())) {
            itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                    .eq(Item::getId, id)
                    .set(Item::getStatus, Item.STATUS_APPROVED)
                    .set(Item::getRejectReason, null));
            notificationService.create(item.getPublisherId(), Notification.TYPE_ITEM_AUDIT,
                    "信息审核通过",
                    "你发布的《" + item.getTitle() + "》已通过审核，现在可以在列表中查看了",
                    id);
        } else {
            if (!StringUtils.hasText(dto.getReason())) {
                throw BusinessException.badRequest("驳回时必须填写原因");
            }
            itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                    .eq(Item::getId, id)
                    .set(Item::getStatus, Item.STATUS_REJECTED)
                    .set(Item::getRejectReason, dto.getReason()));
            notificationService.create(item.getPublisherId(), Notification.TYPE_ITEM_AUDIT,
                    "信息审核未通过",
                    "你发布的《" + item.getTitle() + "》被驳回：" + dto.getReason(),
                    id);
        }
    }

    /**
     * 后台概览数据
     */
    public Map<String, Object> stats() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalItems", count(Wrappers.<Item>lambdaQuery()));
        map.put("pendingItems", count(Wrappers.<Item>lambdaQuery().eq(Item::getStatus, Item.STATUS_PENDING)));
        map.put("approvedItems", count(Wrappers.<Item>lambdaQuery().eq(Item::getStatus, Item.STATUS_APPROVED)));
        map.put("finishedItems", count(Wrappers.<Item>lambdaQuery().eq(Item::getStatus, Item.STATUS_FINISHED)));
        map.put("totalUsers", countUser(Wrappers.<User>lambdaQuery()));
        map.put("totalClaims", countClaim(Wrappers.<Claim>lambdaQuery()));
        map.put("pendingClaims", countClaim(Wrappers.<Claim>lambdaQuery().eq(Claim::getStatus, Claim.STATUS_PENDING)));
        map.put("todayItems", count(Wrappers.<Item>lambdaQuery()
                .ge(Item::getCreateTime, LocalDate.now().atStartOfDay())));

        List<Map<String, Object>> categories = new ArrayList<>();
        for (String code : Arrays.asList("card", "digital", "device", "key", "book", "other")) {
            long c = count(Wrappers.<Item>lambdaQuery().eq(Item::getCategory, code));
            if (c > 0) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("category", code);
                row.put("label", Labels.category(code));
                row.put("count", c);
                categories.add(row);
            }
        }
        map.put("categoryStats", categories);
        return map;
    }

    // ------------------------------------------------------------ 内部实现

    private PageVO<ItemVO> page(ItemQuery query, Long viewerId) {
        LambdaQueryWrapper<Item> wrapper = buildWrapper(query);
        Page<Item> page = new Page<>(query.currentPage(), query.pageSize());
        Page<Item> result = itemMapper.selectPage(page, wrapper);

        Map<Long, User> publishers = loadUsers(result.getRecords().stream()
                .map(Item::getPublisherId).filter(Objects::nonNull).distinct().toList());
        List<ItemVO> records = result.getRecords().stream()
                .map(item -> toVO(item, publishers, viewerId, false))
                .toList();
        return PageVO.of(records, result.getTotal(), query.currentPage(), query.pageSize());
    }

    private LambdaQueryWrapper<Item> buildWrapper(ItemQuery query) {
        LambdaQueryWrapper<Item> wrapper = Wrappers.lambdaQuery();
        String status = StringUtils.hasText(query.getStatus()) ? query.getStatus() : Item.STATUS_APPROVED;
        if (!"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(Item::getStatus, status);
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Item::getType, query.getType());
        }
        if (StringUtils.hasText(query.getCategory())) {
            wrapper.eq(Item::getCategory, query.getCategory());
        }
        if (query.getPublisherId() != null) {
            wrapper.eq(Item::getPublisherId, query.getPublisherId());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Item::getTitle, keyword)
                    .or().like(Item::getDescription, keyword)
                    .or().like(Item::getPlace, keyword));
        }
        if ("view".equalsIgnoreCase(query.getSort())) {
            wrapper.orderByDesc(Item::getViewCount);
        }
        wrapper.orderByDesc(Item::getId);
        return wrapper;
    }

    private Item requireItem(Long id) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw BusinessException.notFound("信息不存在或已被删除");
        }
        return item;
    }

    private void checkOwnerOrAdmin(Item item, Long userId) {
        if (!item.getPublisherId().equals(userId) && !UserContext.isAdmin()) {
            throw BusinessException.forbidden("只能操作自己发布的信息");
        }
    }

    private void applyDTO(Item item, ItemDTO dto) {
        item.setTitle(dto.getTitle());
        item.setType(dto.getType());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        item.setImages(dto.getImages() == null || dto.getImages().isEmpty()
                ? null : String.join(",", dto.getImages()));
        item.setLostTime(dto.getLostTime() == null ? LocalDateTime.now() : dto.getLostTime());
        item.setPlace(dto.getPlace());
        item.setContact(dto.getContact());
    }

    private Map<Long, User> loadUsers(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private ItemVO toVO(Item item, Map<Long, User> publishers, Long viewerId, boolean detail) {
        ItemVO vo = new ItemVO();
        vo.setId(item.getId());
        vo.setTitle(item.getTitle());
        vo.setType(item.getType());
        vo.setTypeText(Labels.type(item.getType()));
        vo.setCategory(item.getCategory());
        vo.setCategoryText(Labels.category(item.getCategory()));
        vo.setDescription(item.getDescription());
        vo.setLostTime(item.getLostTime());
        vo.setPlace(item.getPlace());
        vo.setStatus(item.getStatus());
        vo.setStatusText(Labels.itemStatus(item.getStatus()));
        vo.setRejectReason(item.getRejectReason());
        vo.setViewCount(item.getViewCount());
        vo.setPublisherId(item.getPublisherId());
        vo.setCreateTime(item.getCreateTime());

        List<String> images = StringUtils.hasText(item.getImages())
                ? Arrays.stream(item.getImages().split(",")).filter(StringUtils::hasText).toList()
                : Collections.emptyList();
        vo.setImages(detail ? images : Collections.emptyList());
        vo.setCover(images.isEmpty() ? null : images.get(0));

        User publisher = publishers.get(item.getPublisherId());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherAvatar(publisher.getAvatar());
        }

        boolean owner = viewerId != null && viewerId.equals(item.getPublisherId());
        vo.setOwner(owner);
        if (detail) {
            // 联系方式：登录用户可见完整信息，未登录打码
            boolean canSee = viewerId != null || owner;
            vo.setContact(canSee ? item.getContact() : mask(item.getContact()));
        }
        return vo;
    }

    private String mask(String contact) {
        if (!StringUtils.hasText(contact)) {
            return contact;
        }
        if (contact.matches("^1[3-9]\\d{9}$")) {
            return contact.substring(0, 3) + "****" + contact.substring(7);
        }
        if (contact.length() <= 3) {
            return "***";
        }
        return contact.substring(0, 2) + "***";
    }

    private long count(LambdaQueryWrapper<Item> wrapper) {
        Long value = itemMapper.selectCount(wrapper);
        return value == null ? 0 : value;
    }

    private long countUser(LambdaQueryWrapper<User> wrapper) {
        Long value = userMapper.selectCount(wrapper);
        return value == null ? 0 : value;
    }

    private long countClaim(LambdaQueryWrapper<Claim> wrapper) {
        Long value = claimMapper.selectCount(wrapper);
        return value == null ? 0 : value;
    }
}
