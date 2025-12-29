-- PointQuest schema built from docs/design requirements.

CREATE DATABASE IF NOT EXISTS point_quest
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE point_quest;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS users;
-- 用户与认证
CREATE TABLE users
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    index idx_email (email)
) COMMENT ='用户表：存储登录账号及创建/更新时间';

DROP TABLE IF EXISTS admin_user;
CREATE TABLE admin_user
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    index idx_admin_username (username)
) COMMENT ='管理员账户表：为后台管理提供登录账号与凭据';

-- 任务与提交
DROP TABLE IF EXISTS task;
CREATE TABLE task
(
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no           VARCHAR(64)  NOT NULL UNIQUE DEFAULT (UUID()) COMMENT '任务编号',
    title             VARCHAR(255) NOT NULL,
    description       TEXT,
    point_reward      BIGINT       NOT NULL CHECK (point_reward >= 0),
    deadline          DATETIME,                                                    -- 类型？
    status            VARCHAR(32)  NOT NULL        DEFAULT 'OPEN',                 -- 枚举感觉要改 'OPEN', 'CLOSED', 'ENDED'
    created_by        BIGINT       NOT NULL,
    created_user_type VARCHAR(32)  NOT NULL        DEFAULT 'USER',                 -- USER ADMIN
    created_at        TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_del            TINYINT(1)   NOT NULL        DEFAULT 0,
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES admin_user (id) -- 这个要改
) COMMENT ='任务表：记录可领取任务、时间范围与发布者';

DROP TABLE IF EXISTS task_submission;
CREATE TABLE task_submission
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_no VARCHAR(64) NOT NULL UNIQUE DEFAULT (UUID()),
    task_id       BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    evidence_text TEXT,
    status        VARCHAR(32) NOT NULL        DEFAULT 'PENDING', -- 枚举感觉要改 'PENDING', 'APPROVED', 'REJECTED'
    created_at    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT ='任务提交表：用户提交任务完成证明及审批状态';
CREATE INDEX idx_submission_task_user_status ON task_submission (task_id, user_id, status);

-- 审核记录
DROP TABLE IF EXISTS submission_review;
CREATE TABLE submission_review
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id  BIGINT    NOT NULL UNIQUE,
    reviewer_id    BIGINT    NOT NULL,
    comment        TEXT,
    points_awarded BIGINT    NOT NULL CHECK (points_awarded >= 0),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_submission FOREIGN KEY (submission_id) REFERENCES task_submission (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES admin_user (id)
) COMMENT ='审核记录表：记录管理员对提交的评审与奖励积分';

-- 积分账户与流水
DROP TABLE IF EXISTS point_account;
CREATE TABLE point_account
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT    NOT NULL UNIQUE,
    balance    BIGINT    NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT ='积分账户表：维护用户当前积分余额';

DROP TABLE IF EXISTS point_ledger;
CREATE TABLE point_ledger
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT                                 NOT NULL,
    delta      BIGINT                                 NOT NULL,
    ref_type   ENUM ('SUBMISSION', 'ORDER', 'ADJUST') NOT NULL,
    ref_id     BIGINT                                 NOT NULL,
    remark     VARCHAR(255),
    created_at TIMESTAMP                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT ='积分流水表：记录积分增减来源、备注与时间';
CREATE INDEX idx_ledger_user_created_at ON point_ledger (user_id, created_at);

-- 商品与库存
DROP TABLE IF EXISTS reward;
CREATE TABLE reward
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128)       NOT NULL,
    reward_no   VARCHAR(64)        NOT NULL UNIQUE,
    description TEXT,
    point_cost  BIGINT             NOT NULL CHECK (point_cost >= 0),
    status      ENUM ('ON', 'OFF') NOT NULL DEFAULT 'ON', -- 后端代码中枚举
    created_at  TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_del      TINYINT(1)         NOT NULL DEFAULT 0
) COMMENT ='兑换商品表：维护奖品信息、分类与上下架状态';

-- Reward categories
DROP TABLE IF EXISTS category;
CREATE TABLE category
(
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE
) COMMENT ='Reward category definitions';

-- Reward-category relation
DROP TABLE IF EXISTS reward_category;
CREATE TABLE reward_category
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    reward_id   BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT fk_reward_category_reward FOREIGN KEY (reward_id) REFERENCES reward (id),
    CONSTRAINT fk_reward_category_category FOREIGN KEY (category_id) REFERENCES category (id)
) COMMENT ='Mapping between reward and category';

DROP TABLE IF EXISTS reward_inventory;
CREATE TABLE reward_inventory
(
    reward_id  BIGINT PRIMARY KEY,
    stock      INT       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    version    BIGINT    NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT ='库存表：记录每个奖品的库存数量与版本号';

-- 活动奖池
DROP TABLE IF EXISTS pool;
CREATE TABLE pool
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    pool_no     VARCHAR(64)        NOT NULL UNIQUE,
    title       VARCHAR(128)       NOT NULL,
    description TEXT               NULL,
    start_at    DATETIME,
    end_at      DATETIME,
    status      ENUM ('ON', 'OFF') NOT NULL DEFAULT 'OFF',
    point_cost  BIGINT UNSIGNED    NOT NULL CHECK (point_cost >= 0) COMMENT '抽卡花费',
    type        VARCHAR(32)        NOT NULL DEFAULT 'NORMAL' COMMENT '卡池类型',
    created_at  TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT ='活动奖池：定义活动窗口与奖池状态';

DROP TABLE IF EXISTS pool_item;
CREATE TABLE pool_item
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    pool_id   BIGINT NOT NULL,
    reward_id BIGINT NOT NULL,
    sort_no   INT    NOT NULL DEFAULT 0,
    weight    BIGINT NOT NULL DEFAULT 0 COMMENT '权重',
    CONSTRAINT uq_pool_reward UNIQUE (pool_id, reward_id),
    CONSTRAINT fk_pool_item_pool FOREIGN KEY (pool_id) REFERENCES pool (id),
    CONSTRAINT fk_pool_item_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT ='奖池条目：记录奖池与奖品的关联及展示顺序';

-- 订单与支付
DROP TABLE IF EXISTS orders;
CREATE TABLE orders
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no     VARCHAR(64)      NOT NULL UNIQUE,
    user_id      BIGINT           NOT NULL,
    total_points BIGINT           NOT NULL CHECK (total_points >= 0),
    address      VARCHAR(255)     NULL,
    status       ENUM ('CREATED', 'PROCESSING', 'SHIPPED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT ='订单表：记录用户兑换订单与收货信息';
CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at);

DROP TABLE IF EXISTS order_item;
CREATE TABLE order_item
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id             BIGINT       NOT NULL,
    reward_id            BIGINT       NOT NULL,
    reward_name_snapshot VARCHAR(128) NOT NULL,
    point_cost_snapshot  BIGINT       NOT NULL CHECK (point_cost_snapshot >= 0),
    qty                  INT          NOT NULL CHECK (qty >= 1),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT ='订单明细表：记录每个订单包含的奖品快照及数量';
CREATE INDEX idx_order_item_order ON order_item (order_id);

DROP TABLE IF EXISTS payment;
CREATE TABLE payment
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL UNIQUE,
    card_last4 CHAR(4),
    pay_method ENUM ('MOCK') NOT NULL DEFAULT 'MOCK',
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) COMMENT ='支付记录表：模拟支付渠道的结果信息';

-- 站内消息
DROP TABLE IF EXISTS message;
CREATE TABLE message
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    sender_id   BIGINT,
    receiver_id BIGINT       NOT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at     DATETIME,
    CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_message_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
) COMMENT ='站内消息表：用于用户间通知与已读状态';
CREATE INDEX idx_message_receiver_read ON message (receiver_id, is_read, created_at);

-- 池子抽卡记录
DROP TABLE IF EXISTS pool_draw;
CREATE TABLE pool_draw
(
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id              BIGINT    NOT NULL,
    pool_id              BIGINT    NOT NULL,
    reward_id            BIGINT    NOT NULL,
    reward_name_snapshot VARCHAR(128),
    reward_no_snapshot   VARCHAR(64),
    point_cost           BIGINT    NOT NULL CHECK (point_cost >= 0),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pool_draw_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_pool_draw_pool FOREIGN KEY (pool_id) REFERENCES pool (id),
    CONSTRAINT fk_pool_draw_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT = '卡池抽卡记录';
CREATE INDEX idx_pool_draw_user_created ON pool_draw (user_id, created_at);

SET FOREIGN_KEY_CHECKS = 1;
