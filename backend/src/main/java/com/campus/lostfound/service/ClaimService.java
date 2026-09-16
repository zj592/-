package com.campus.lostfound.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.Labels;
import com.campus.lostfound.dto.ClaimDTO;
import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.Notification;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.UserContext;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 认领申请：提交、撤销、发布者处理
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private final ClaimMapper claimMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    /**
     * 提交认领申请
     */
    public ClaimVO submit(Long userId, ClaimDTO dto) {
        Item item = itemMapper.selectById(dto.getItemId());
        if (item == null) {
            throw BusinessException.notFound("信息不存在或已被删除");
        }
        if (item.getPublisherId().equals(userId)) {
            throw BusinessException.badRequest("不能认领自己发布的信息");
        }
        if (!Item.STATUS_APPROVED.equals(item.getStatus())) {
            throw BusinessException.badRequest("该信息当前不可申请认领");
        }
        Long exists = claimMapper.selectCount(Wrappers.<Claim>lambdaQuery()
                .eq(Claim::getItemId, dto.getItemId())
                .eq(Claim::getClaimantId, userId)
                .ne(Claim::getStatus, Claim.STATUS_CANCELED));
        if (exists != null && exists > 0) {
            throw BusinessException.badRequest("你已经提交过申请，请等待发布者处理");
        }

        Claim claim = new Claim();
        claim.setItemId(dto.getItemId());
        claim.setClaimantId(userId);
        claim.setDescription(dto.getDescription());
        claim.setContact(dto.getContact());
        claim.setStatus(Claim.STATUS_PENDING);
        claimMapper.insert(claim);

        // 通知发布者：有人来认领了
        notificationService.create(item.getPublisherId(), Notification.TYPE_CLAIM_RECEIVED,
                "收到新的认领申请",
                "有人申请认领《" + item.getTitle() + "》，请到「我收到的申请」里处理",
                claim.getId());

        return toVO(claim, Collections.singletonMap(item.getId(), item), Collections.emptyMap());
    }

    /**
     * 我提交的申请
     */
    public PageVO<ClaimVO> myClaims(Long userId, String status, int page, int size) {
        LambdaQueryWrapper<Claim> wrapper = Wrappers.<Claim>lambdaQuery()
                .eq(Claim::getClaimantId, userId)
                .orderByDesc(Claim::getId);
        applyStatus(wrapper, status);
        return page(wrapper, page, size);
    }

    /**
     * 我发布的信息收到的申请（待我处理）
     */
    public PageVO<ClaimVO> receivedClaims(Long userId, String status, int page, int size) {
        List<Long> itemIds = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                        .select(Item::getId)
                        .eq(Item::getPublisherId, userId))
                .stream().map(Item::getId).toList();
        if (itemIds.isEmpty()) {
            return PageVO.of(Collections.emptyList(), 0, page, size);
        }
        LambdaQueryWrapper<Claim> wrapper = Wrappers.<Claim>lambdaQuery()
                .in(Claim::getItemId, itemIds)
                .orderByDesc(Claim::getId);
        applyStatus(wrapper, status);
        return page(wrapper, page, size);
    }

    /**
     * 申请人撤销申请（仅待处理状态可撤销）
     */
    public void cancel(Long claimId, Long userId) {
        Claim claim = requireClaim(claimId);
        if (!claim.getClaimantId().equals(userId)) {
            throw BusinessException.forbidden("只能撤销自己提交的申请");
        }
        if (!Claim.STATUS_PENDING.equals(claim.getStatus())) {
            throw BusinessException.badRequest("该申请已被处理，无法撤销");
        }
        claimMapper.update(null, Wrappers.<Claim>lambdaUpdate()
                .eq(Claim::getId, claimId)
                .set(Claim::getStatus, Claim.STATUS_CANCELED));
    }

    /**
     * 发布者处理申请：同意后该信息变为"已完成"，同一条信息的其他申请自动驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(Long claimId, Long userId, boolean approved, String remark) {
        Claim claim = requireClaim(claimId);
        Item item = itemMapper.selectById(claim.getItemId());
        if (item == null) {
            throw BusinessException.notFound("关联信息不存在");
        }
        if (!item.getPublisherId().equals(userId)) {
            throw BusinessException.forbidden("只有发布者可以处理该申请");
        }
        if (!Claim.STATUS_PENDING.equals(claim.getStatus())) {
            throw BusinessException.badRequest("该申请已处理过");
        }

        // 同意之前先把其他待处理申请捞出来，一会儿要逐个通知
        List<Claim> others = approved
                ? claimMapper.selectList(Wrappers.<Claim>lambdaQuery()
                .eq(Claim::getItemId, item.getId())
                .eq(Claim::getStatus, Claim.STATUS_PENDING)
                .ne(Claim::getId, claimId))
                : Collections.emptyList();

        String finalRemark = StringUtils.hasText(remark)
                ? remark
                : (approved ? "已确认，请联系我取回" : "信息不符，暂不通过");

        claimMapper.update(null, Wrappers.<Claim>lambdaUpdate()
                .eq(Claim::getId, claimId)
                .set(Claim::getStatus, approved ? Claim.STATUS_APPROVED : Claim.STATUS_REJECTED)
                .set(Claim::getAuditRemark, finalRemark));

        if (approved) {
            itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                    .eq(Item::getId, item.getId())
                    .set(Item::getStatus, Item.STATUS_FINISHED));
            if (!others.isEmpty()) {
                claimMapper.update(null, Wrappers.<Claim>lambdaUpdate()
                        .eq(Claim::getItemId, item.getId())
                        .eq(Claim::getStatus, Claim.STATUS_PENDING)
                        .ne(Claim::getId, claimId)
                        .set(Claim::getStatus, Claim.STATUS_REJECTED)
                        .set(Claim::getAuditRemark, "物品已由其他同学认领"));
            }
        }

        // 通知申请人处理结果
        notificationService.create(claim.getClaimantId(), Notification.TYPE_CLAIM_RESULT,
                approved ? "认领申请已通过" : "认领申请未通过",
                approved
                        ? "你对《" + item.getTitle() + "》的认领申请已通过，发布者备注：" + finalRemark
                        : "你对《" + item.getTitle() + "》的认领申请被驳回：" + finalRemark,
                claim.getId());

        // 物品已归还他人，其余申请人也要收到结果
        for (Claim other : others) {
            notificationService.create(other.getClaimantId(), Notification.TYPE_CLAIM_RESULT,
                    "认领申请未通过",
                    "《" + item.getTitle() + "》已由其他同学认领，可以再看看其他相似信息",
                    other.getId());
        }
    }

    // ------------------------------------------------------------ 内部实现

    private void applyStatus(LambdaQueryWrapper<Claim> wrapper, String status) {
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(Claim::getStatus, status);
        }
    }

    private PageVO<ClaimVO> page(LambdaQueryWrapper<Claim> wrapper, int page, int size) {
        Page<Claim> result = claimMapper.selectPage(new Page<>(Math.max(page, 1), Math.min(size, 50)), wrapper);
        List<Claim> records = result.getRecords();

        Map<Long, Item> items = loadItems(records.stream().map(Claim::getItemId).distinct().toList());
        Map<Long, User> users = loadUsers(records.stream().map(Claim::getClaimantId).distinct().toList());
        List<ClaimVO> list = records.stream().map(c -> toVO(c, items, users)).toList();
        return PageVO.of(list, result.getTotal(), Math.max(page, 1), Math.min(size, 50));
    }

    private Claim requireClaim(Long id) {
        Claim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw BusinessException.notFound("申请记录不存在");
        }
        return claim;
    }

    private Map<Long, Item> loadItems(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return itemMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Item::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, User> loadUsers(List<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    private ClaimVO toVO(Claim claim, Map<Long, Item> items, Map<Long, User> users) {
        ClaimVO vo = new ClaimVO();
        vo.setId(claim.getId());
        vo.setItemId(claim.getItemId());
        vo.setClaimantId(claim.getClaimantId());
        vo.setDescription(claim.getDescription());
        vo.setContact(claim.getContact());
        vo.setStatus(claim.getStatus());
        vo.setStatusText(Labels.claimStatus(claim.getStatus()));
        vo.setAuditRemark(claim.getAuditRemark());
        vo.setCreateTime(claim.getCreateTime());

        Item item = items.get(claim.getItemId());
        if (item != null) {
            vo.setItemTitle(item.getTitle());
            vo.setItemType(item.getType());
            vo.setItemTypeText(Labels.type(item.getType()));
            vo.setItemStatus(item.getStatus());
            if (StringUtils.hasText(item.getImages())) {
                vo.setItemCover(item.getImages().split(",")[0]);
            }
        }
        User claimant = users.get(claim.getClaimantId());
        if (claimant != null) {
            vo.setClaimantNickname(claimant.getNickname());
        }
        // 详情/列表里带出的 claimant 已由调用方过滤，这里只保留展示需要的字段
        if (UserContext.getOrNull() == null) {
            vo.setContact(null);
        }
        return vo;
    }

    /**
     * 供详情接口判断"当前用户是否已申请过"
     */
    public boolean hasClaimed(Long itemId, Long userId) {
        if (itemId == null || userId == null) {
            return false;
        }
        Long count = claimMapper.selectCount(Wrappers.<Claim>lambdaQuery()
                .eq(Claim::getItemId, itemId)
                .eq(Claim::getClaimantId, userId)
                .ne(Claim::getStatus, Claim.STATUS_CANCELED));
        return count != null && count > 0;
    }

    /**
     * 管理员查看全部申请
     */
    public PageVO<ClaimVO> allClaims(String status, int page, int size) {
        LambdaQueryWrapper<Claim> wrapper = Wrappers.<Claim>lambdaQuery().orderByDesc(Claim::getId);
        applyStatus(wrapper, status);
        return page(wrapper, page, size);
    }
}
