-- 校园失物招领平台 - 建库建表
-- MySQL 8.0+，执行：mysql -uroot -p < db/init.sql
-- 只有表结构，演示账号和数据由后端启动时写入（见 DataInitializer）

CREATE DATABASE IF NOT EXISTS `lost_found`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `lost_found`;

DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `claim`;
DROP TABLE IF EXISTS `item`;
DROP TABLE IF EXISTS `user`;

-- ------------------------------------------------------------
-- 用户表
-- ------------------------------------------------------------
CREATE TABLE `user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文（微信登录用户写入随机密码）',
    `openid`      VARCHAR(64)           DEFAULT NULL COMMENT '微信小程序 openid',
    `nickname`    VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '联系方式',
    `campus`      VARCHAR(50)           DEFAULT NULL COMMENT '常活动区域/校区',
    `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT-学生 ADMIN-管理员',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ------------------------------------------------------------
-- 失物/寻物信息表
--   type   = LOST  寻物启事（我丢了东西）
--   type   = FOUND 失物招领（我捡到东西）
--   status = PENDING 待审核 / APPROVED 已通过 / REJECTED 已驳回 / FINISHED 已完成
-- ------------------------------------------------------------
CREATE TABLE `item`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `title`         VARCHAR(100) NOT NULL COMMENT '标题',
    `type`          VARCHAR(10)  NOT NULL COMMENT 'LOST-寻物 FOUND-招领',
    `category`      VARCHAR(20)  NOT NULL COMMENT '分类：card-证件卡类 digital-数码 device-生活用品 key-钥匙 other-其他',
    `description`   TEXT COMMENT '详细描述',
    `images`        VARCHAR(1000)         DEFAULT NULL COMMENT '图片地址，多个用英文逗号分隔',
    `lost_time`     DATETIME              DEFAULT NULL COMMENT '丢失/拾取时间',
    `place`         VARCHAR(100)          DEFAULT NULL COMMENT '丢失/拾取地点',
    `contact`       VARCHAR(100)          DEFAULT NULL COMMENT '联系方式（微信号/手机号/QQ）',
    `publisher_id`  BIGINT       NOT NULL COMMENT '发布人 id',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '审核状态',
    `reject_reason` VARCHAR(255)          DEFAULT NULL COMMENT '驳回原因',
    `view_count`    INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_type` (`status`, `type`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_category` (`category`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='失物招领信息表';

-- ------------------------------------------------------------
-- 认领申请表
--   status = PENDING 待处理 / APPROVED 已通过 / REJECTED 已驳回 / CANCELED 已撤销
-- ------------------------------------------------------------
CREATE TABLE `claim`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`      BIGINT       NOT NULL COMMENT '关联的信息 id',
    `claimant_id`  BIGINT       NOT NULL COMMENT '申请人 id',
    `description`  VARCHAR(500) NOT NULL COMMENT '认领说明 / 物品特征凭证',
    `contact`      VARCHAR(100)          DEFAULT NULL COMMENT '申请人联系方式',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
    `audit_remark` VARCHAR(255)          DEFAULT NULL COMMENT '发布者处理备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_claimant` (`item_id`, `claimant_id`),
    KEY `idx_claimant` (`claimant_id`),
    KEY `idx_item` (`item_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='认领申请表';

-- ------------------------------------------------------------
-- 站内通知表
--   type = CLAIM_RECEIVED 收到新的认领申请（通知发布者）
--   type = CLAIM_RESULT   认领结果（通知申请人）
--   type = ITEM_AUDIT     信息审核结果（通知发布者）
-- ------------------------------------------------------------
CREATE TABLE `notification`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '接收人 id',
    `type`        VARCHAR(20)  NOT NULL COMMENT '通知类型',
    `title`       VARCHAR(100) NOT NULL COMMENT '标题',
    `content`     VARCHAR(300)          DEFAULT NULL COMMENT '正文',
    `related_id`  BIGINT                DEFAULT NULL COMMENT '关联业务 id（信息 id 或申请 id）',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读 1-已读',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='站内通知表';

-- 演示账号见 README.md
