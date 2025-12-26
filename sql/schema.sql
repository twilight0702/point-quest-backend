-- PointQuest schema built from docs/design requirements.

CREATE DATABASE IF NOT EXISTS point_quest
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE point_quest;

SET FOREIGN_KEY_CHECKS = 0;

-- 用户与认证
CREATE TABLE users (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    index idx_email (email)
) COMMENT='用户表：存储登录账号、角色及创建/更新时间';

-- 任务与提交
CREATE TABLE task (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    point_reward BIGINT       NOT NULL CHECK (point_reward >= 0),
    deadline     DATETIME,
    status       ENUM('OPEN', 'CLOSED', 'ENDED') NOT NULL DEFAULT 'OPEN',
    created_by   BIGINT       NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_creator FOREIGN KEY (created_by) REFERENCES users (id)
) COMMENT='任务表：记录可领取任务、时间范围与发布者';

CREATE TABLE task_submission (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id       BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    evidence_url  VARCHAR(512),
    evidence_text TEXT,
    status        ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_submission_task FOREIGN KEY (task_id) REFERENCES task (id),
    CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='任务提交表：用户提交任务完成证明及审批状态';
CREATE INDEX idx_submission_task_user_status ON task_submission (task_id, user_id, status);

-- 审核记录
CREATE TABLE submission_review (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id  BIGINT    NOT NULL UNIQUE,
    reviewer_id    BIGINT    NOT NULL,
    comment        TEXT,
    points_awarded BIGINT    NOT NULL CHECK (points_awarded >= 0),
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_submission FOREIGN KEY (submission_id) REFERENCES task_submission (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
) COMMENT='审核记录表：记录管理员对提交的评审与奖励积分';

-- 积分账户与流水
CREATE TABLE point_account (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT    NOT NULL UNIQUE,
    balance    BIGINT    NOT NULL DEFAULT 0 CHECK (balance >= 0),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='积分账户表：维护用户当前积分余额';

CREATE TABLE point_ledger (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT                                         NOT NULL,
    delta      BIGINT                                         NOT NULL,
    ref_type   ENUM('SUBMISSION', 'ORDER', 'ADJUST')          NOT NULL,
    ref_id     BIGINT                                         NOT NULL,
    remark     VARCHAR(255),
    created_at TIMESTAMP                                      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='积分流水表：记录积分增减来源、备注与时间';
CREATE INDEX idx_ledger_user_created_at ON point_ledger (user_id, created_at);

-- 商品与库存
CREATE TABLE reward (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    description TEXT,
    point_cost  BIGINT       NOT NULL CHECK (point_cost >= 0),
    category    VARCHAR(64),
    status      ENUM('ON', 'OFF') NOT NULL DEFAULT 'ON',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='兑换商品表：维护奖品信息、分类与上下架状态';

CREATE TABLE reward_inventory (
    reward_id BIGINT PRIMARY KEY,
    stock     INT        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    version   BIGINT     NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT='库存表：记录每个奖品的库存数量与版本号';

-- 活动奖池
CREATE TABLE pool (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    title      VARCHAR(128) NOT NULL,
    start_at   DATETIME,
    end_at     DATETIME,
    status     ENUM('ON', 'OFF') NOT NULL DEFAULT 'OFF',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='活动奖池：定义活动窗口与奖池状态';

CREATE TABLE pool_item (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    pool_id   BIGINT    NOT NULL,
    reward_id BIGINT    NOT NULL,
    sort_no   INT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_pool_reward UNIQUE (pool_id, reward_id),
    CONSTRAINT fk_pool_item_pool FOREIGN KEY (pool_id) REFERENCES pool (id),
    CONSTRAINT fk_pool_item_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT='奖池条目：记录奖池与奖品的关联及展示顺序';

-- 订单与支付
CREATE TABLE orders (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no     VARCHAR(64) NOT NULL UNIQUE,
    user_id      BIGINT      NOT NULL,
    total_points BIGINT      NOT NULL CHECK (total_points >= 0),
    address_json JSON        NOT NULL,
    status       ENUM('CREATED') NOT NULL DEFAULT 'CREATED',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_address_json_valid CHECK (JSON_VALID(address_json)),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users (id)
) COMMENT='订单表：记录用户兑换订单与收货信息';
CREATE INDEX idx_orders_user_created_at ON orders (user_id, created_at);

CREATE TABLE order_item (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id              BIGINT       NOT NULL,
    reward_id             BIGINT       NOT NULL,
    reward_name_snapshot  VARCHAR(128) NOT NULL,
    point_cost_snapshot   BIGINT       NOT NULL CHECK (point_cost_snapshot >= 0),
    qty                   INT          NOT NULL CHECK (qty >= 1),
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_reward FOREIGN KEY (reward_id) REFERENCES reward (id)
) COMMENT='订单明细表：记录每个订单包含的奖品快照及数量';
CREATE INDEX idx_order_item_order ON order_item (order_id);

CREATE TABLE payment (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id   BIGINT      NOT NULL UNIQUE,
    card_last4 CHAR(4),
    pay_method ENUM('MOCK') NOT NULL DEFAULT 'MOCK',
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) COMMENT='支付记录表：模拟支付渠道的结果信息';

-- 站内消息
CREATE TABLE message (
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
) COMMENT='站内消息表：用于用户间通知与已读状态';
CREATE INDEX idx_message_receiver_read ON message (receiver_id, is_read, created_at);

SET FOREIGN_KEY_CHECKS = 1;
