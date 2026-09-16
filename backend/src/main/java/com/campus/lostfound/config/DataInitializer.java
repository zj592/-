package com.campus.lostfound.config;

import com.campus.lostfound.entity.Claim;
import com.campus.lostfound.entity.Item;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.mapper.ClaimMapper;
import com.campus.lostfound.mapper.ItemMapper;
import com.campus.lostfound.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时初始化演示数据
 * 账号密码必须用 BCrypt 生成，所以不写在 init.sql 里
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ItemMapper itemMapper;
    private final ClaimMapper claimMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init-data:true}")
    private boolean initData;

    @Override
    public void run(String... args) {
        if (!initData) {
            return;
        }
        Long userCount = userMapper.selectCount(null);
        if (userCount == null || userCount == 0) {
            initUsers();
        }
        Long itemCount = itemMapper.selectCount(null);
        if (itemCount == null || itemCount == 0) {
            initItems();
        }
        // 认领申请要在信息之后初始化，否则查不到可关联的信息
        Long claimCount = claimMapper.selectCount(null);
        if (claimCount == null || claimCount == 0) {
            initClaim();
        }
    }

    private void initUsers() {
        insertUser("admin", "admin123", "失物招领管理员", User.ROLE_ADMIN, "行政楼");
        insertUser("student", "123456", "李同学", User.ROLE_STUDENT, "东区宿舍");
        insertUser("lin", "123456", "林同学", User.ROLE_STUDENT, "图书馆");
        insertUser("chen", "123456", "陈同学", User.ROLE_STUDENT, "西区教学楼");
        log.info("已初始化演示账号：admin/admin123（管理员）、student/123456、lin/123456、chen/123456");
    }

    /**
     * 一条演示用的待处理认领申请：林同学申请认领李同学发布的信息
     */
    private void initClaim() {
        Long linId = idOf("lin");
        Item target = itemMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<Item>lambdaQuery()
                        .eq(Item::getPublisherId, idOf("student"))
                        .orderByAsc(Item::getId)
                        .last("limit 1"));
        if (target == null || linId == null) {
            return;
        }
        Claim claim = new Claim();
        claim.setItemId(target.getId());
        claim.setClaimantId(linId);
        claim.setDescription("这个应该是我丢的，卡套背面贴了一张蓝色的小贴纸，卡号后四位 6231");
        claim.setContact("lin_wx");
        claim.setStatus(Claim.STATUS_PENDING);
        claimMapper.insert(claim);
        log.info("已初始化演示认领申请：信息 id={}，申请人 id={}", target.getId(), linId);
    }

    private Long insertUser(String username, String rawPassword, String nickname, String role, String campus) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setCampus(campus);
        user.setPhone("15900000000");
        user.setRole(role);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    private void initItems() {
        Long studentId = idOf("student");
        Long linId = idOf("lin");
        Long chenId = idOf("chen");
        if (studentId == null || linId == null) {
            return;
        }

        insertItem("丢失一张校园一卡通，卡面有划痕", Item.TYPE_LOST, "card",
                "昨天下午在食堂二楼吃完饭就不见了，卡号后四位 6231，卡套是蓝色的，捡到请联系我，必有酬谢。",
                "东区第二食堂", "student", studentId, Item.STATUS_APPROVED, 36);

        insertItem("捡到一副白色蓝牙耳机", Item.TYPE_FOUND, "digital",
                "在图书馆三楼靠窗的位置看到的，充电盒是白色的，已经交到图书馆一楼失物招领处，可以凭购买记录认领。",
                "图书馆三楼自习区", "lin", linId, Item.STATUS_APPROVED, 58);

        insertItem("寻找一把宿舍钥匙（带小熊挂件）", Item.TYPE_LOST, "key",
                "钥匙串上有一个黄色小熊挂件和两把钥匙，可能掉在操场或者回宿舍的路上了。",
                "东区操场", "student", studentId, Item.STATUS_APPROVED, 12);

        insertItem("捡到黑色雨伞一把", Item.TYPE_FOUND, "device",
                "教学楼 A302 教室捡到的，长柄全黑，伞套还在，先放在教室讲台下面了。",
                "教学楼 A302", "chen", chenId, Item.STATUS_APPROVED, 7);

        insertItem("丢失高等数学习题册（写满笔记）", Item.TYPE_LOST, "book",
                "封面是绿色的同济第七版，里面全是我的复习笔记，下周就要考试了，捡到的同学拜托联系我。",
                "图书馆四楼", "lin", linId, Item.STATUS_PENDING, 0);

        insertItem("出售旧教材（内容不合规，示例驳回）", Item.TYPE_FOUND, "other",
                "这是一条用于演示审核驳回流程的数据，内容是广告，不符合社区规范。",
                "线上", "chen", chenId, Item.STATUS_REJECTED, 0);
    }

    private void insertItem(String title, String type, String category, String description,
                            String place, String contact, Long publisherId, String status, int viewCount) {
        Item item = new Item();
        item.setTitle(title);
        item.setType(type);
        item.setCategory(category);
        item.setDescription(description);
        item.setPlace(place);
        item.setContact(contact);
        item.setPublisherId(publisherId);
        item.setStatus(status);
        item.setViewCount(viewCount);
        item.setLostTime(LocalDateTime.now().minusDays(2));
        if (Item.STATUS_REJECTED.equals(status)) {
            item.setRejectReason("内容与失物招领无关，疑似广告");
        }
        itemMapper.insert(item);
    }

    private Long idOf(String username) {
        User user = userMapper.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<User>lambdaQuery()
                        .eq(User::getUsername, username));
        return user == null ? null : user.getId();
    }
}
