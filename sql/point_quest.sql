/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80404 (8.4.4)
 Source Host           : localhost:3306
 Source Schema         : point_quest

 Target Server Type    : MySQL
 Target Server Version : 80404 (8.4.4)
 File Encoding         : 65001

 Date: 29/12/2025 17:06:10
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin_user
-- ----------------------------
DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_admin_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员账户表：为后台管理提供登录账号与凭据' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of admin_user
-- ----------------------------
INSERT INTO `admin_user` VALUES (1, '123', '$2a$10$cvIwjYpgubBMTuxrgSCSOuoK5cFMbx7t.Fg32xCS3xU35wzogrmjm', '2025-12-28 00:48:56', '2025-12-28 00:53:19');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Reward category definitions' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (1, '实体周边');
INSERT INTO `category` VALUES (2, '虚拟兑换');

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `sender_id` bigint NULL DEFAULT NULL,
  `receiver_id` bigint NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `read_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_message_sender`(`sender_id` ASC) USING BTREE,
  INDEX `idx_message_receiver_read`(`receiver_id` ASC, `is_read` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '站内消息表：用于用户间通知与已读状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of message
-- ----------------------------

-- ----------------------------
-- Table structure for order_item
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `reward_id` bigint NOT NULL,
  `reward_name_snapshot` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `point_cost_snapshot` bigint NOT NULL,
  `qty` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_order_item_reward`(`reward_id` ASC) USING BTREE,
  INDEX `idx_order_item_order`(`order_id` ASC) USING BTREE,
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_order_item_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `order_item_chk_1` CHECK (`point_cost_snapshot` >= 0),
  CONSTRAINT `order_item_chk_2` CHECK (`qty` >= 1)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单明细表：记录每个订单包含的奖品快照及数量' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of order_item
-- ----------------------------

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  `total_points` bigint NOT NULL,
  `address_json` json NOT NULL,
  `status` enum('CREATED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CREATED',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_orders_user_created_at`(`user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_address_json_valid` CHECK (json_valid(`address_json`)),
  CONSTRAINT `orders_chk_1` CHECK (`total_points` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '订单表：记录用户兑换订单与收货信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of orders
-- ----------------------------

-- ----------------------------
-- Table structure for payment
-- ----------------------------
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `card_last4` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `pay_method` enum('MOCK') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MOCK',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `order_id`(`order_id` ASC) USING BTREE,
  CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付记录表：模拟支付渠道的结果信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment
-- ----------------------------

-- ----------------------------
-- Table structure for point_account
-- ----------------------------
DROP TABLE IF EXISTS `point_account`;
CREATE TABLE `point_account`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `balance` bigint NOT NULL DEFAULT 0,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_account_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `point_account_chk_1` CHECK (`balance` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分账户表：维护用户当前积分余额' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of point_account
-- ----------------------------
INSERT INTO `point_account` VALUES (2, 2, 50, '2025-12-29 16:01:55');

-- ----------------------------
-- Table structure for point_ledger
-- ----------------------------
DROP TABLE IF EXISTS `point_ledger`;
CREATE TABLE `point_ledger`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `delta` bigint NOT NULL,
  `ref_type` enum('SUBMISSION','ORDER','ADJUST') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `ref_id` bigint NOT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ledger_user_created_at`(`user_id` ASC, `created_at` ASC) USING BTREE,
  CONSTRAINT `fk_ledger_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积分流水表：记录积分增减来源、备注与时间' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of point_ledger
-- ----------------------------

-- ----------------------------
-- Table structure for pool
-- ----------------------------
DROP TABLE IF EXISTS `pool`;
CREATE TABLE `pool`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pool_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `start_at` datetime NULL DEFAULT NULL,
  `end_at` datetime NULL DEFAULT NULL,
  `status` enum('ON','OFF') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OFF',
  `point_cost` bigint UNSIGNED NOT NULL COMMENT '抽卡花费',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT '卡池类型',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `pool_no`(`pool_no` ASC) USING BTREE,
  CONSTRAINT `pool_chk_1` CHECK (`point_cost` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '活动奖池：定义活动窗口与奖池状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pool
-- ----------------------------
INSERT INTO `pool` VALUES (1, '1e90f2e7-6a3f-46a4-8769-d879c1acf26d', '常驻卡池', '是常驻卡池！', '2025-12-26 05:46:25', '2029-05-31 10:17:09', 'ON', 12, 'TYPE', '2025-12-29 13:24:44', '2025-12-29 16:54:32');

-- ----------------------------
-- Table structure for pool_item
-- ----------------------------
DROP TABLE IF EXISTS `pool_item`;
CREATE TABLE `pool_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `pool_id` bigint NOT NULL,
  `reward_id` bigint NOT NULL,
  `sort_no` int NOT NULL DEFAULT 0,
  `weight` bigint NOT NULL DEFAULT 0 COMMENT '权重',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uq_pool_reward`(`pool_id` ASC, `reward_id` ASC) USING BTREE,
  INDEX `fk_pool_item_reward`(`reward_id` ASC) USING BTREE,
  CONSTRAINT `fk_pool_item_pool` FOREIGN KEY (`pool_id`) REFERENCES `pool` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_pool_item_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '奖池条目：记录奖池与奖品的关联及展示顺序' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of pool_item
-- ----------------------------
INSERT INTO `pool_item` VALUES (3, 1, 3, 5, 1);
INSERT INTO `pool_item` VALUES (4, 1, 4, 2, 1);

-- ----------------------------
-- Table structure for reward
-- ----------------------------
DROP TABLE IF EXISTS `reward`;
CREATE TABLE `reward`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `point_cost` bigint NOT NULL,
  `status` enum('ON','OFF') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ON',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_del` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `reward_no`(`reward_no` ASC) USING BTREE,
  CONSTRAINT `reward_chk_1` CHECK (`point_cost` >= 0)
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '兑换商品表：维护奖品信息、分类与上下架状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reward
-- ----------------------------
INSERT INTO `reward` VALUES (3, '徽章', '53c3fe7b-978c-4913-b7e9-0ca09dc7a6f5', 'IPP的徽章！', 20, 'ON', '2025-12-29 11:39:33', '2025-12-29 16:02:23', 0);
INSERT INTO `reward` VALUES (4, '钥匙扣', '5b4b2c62-4309-490a-bec4-e36d7453b757', '一个好看的钥匙扣', 20, 'ON', '2025-12-29 13:39:23', '2025-12-29 16:02:42', 0);

-- ----------------------------
-- Table structure for reward_category
-- ----------------------------
DROP TABLE IF EXISTS `reward_category`;
CREATE TABLE `reward_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reward_id` bigint NOT NULL,
  `category_id` bigint NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_reward_category_reward`(`reward_id` ASC) USING BTREE,
  INDEX `fk_reward_category_category`(`category_id` ASC) USING BTREE,
  CONSTRAINT `fk_reward_category_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_reward_category_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'Mapping between reward and category' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reward_category
-- ----------------------------
INSERT INTO `reward_category` VALUES (1, 3, 1);
INSERT INTO `reward_category` VALUES (2, 4, 1);

-- ----------------------------
-- Table structure for reward_inventory
-- ----------------------------
DROP TABLE IF EXISTS `reward_inventory`;
CREATE TABLE `reward_inventory`  (
  `reward_id` bigint NOT NULL,
  `stock` int NOT NULL DEFAULT 0,
  `version` bigint NOT NULL DEFAULT 0,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`reward_id`) USING BTREE,
  CONSTRAINT `fk_inventory_reward` FOREIGN KEY (`reward_id`) REFERENCES `reward` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `reward_inventory_chk_1` CHECK (`stock` >= 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '库存表：记录每个奖品的库存数量与版本号' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of reward_inventory
-- ----------------------------
INSERT INTO `reward_inventory` VALUES (3, 20, 3, '2025-12-29 11:56:43');
INSERT INTO `reward_inventory` VALUES (4, 20, 0, '2025-12-29 13:39:23');

-- ----------------------------
-- Table structure for submission_review
-- ----------------------------
DROP TABLE IF EXISTS `submission_review`;
CREATE TABLE `submission_review`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_id` bigint NOT NULL,
  `reviewer_id` bigint NOT NULL,
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `points_awarded` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `submission_id`(`submission_id` ASC) USING BTREE,
  INDEX `fk_review_reviewer`(`reviewer_id` ASC) USING BTREE,
  CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `admin_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_review_submission` FOREIGN KEY (`submission_id`) REFERENCES `task_submission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `submission_review_chk_1` CHECK (`points_awarded` >= 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '审核记录表：记录管理员对提交的评审与奖励积分' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of submission_review
-- ----------------------------

-- ----------------------------
-- Table structure for task
-- ----------------------------
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT (uuid()) COMMENT '任务编号',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `point_reward` bigint NOT NULL,
  `deadline` datetime NULL DEFAULT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN',
  `created_by` bigint NOT NULL,
  `created_user_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_del` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `task_no`(`task_no` ASC) USING BTREE,
  INDEX `fk_task_creator`(`created_by` ASC) USING BTREE,
  CONSTRAINT `fk_task_creator` FOREIGN KEY (`created_by`) REFERENCES `admin_user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `task_chk_1` CHECK (`point_reward` >= 0)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '任务表：记录可领取任务、时间范围与发布者' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task
-- ----------------------------

-- ----------------------------
-- Table structure for task_submission
-- ----------------------------
DROP TABLE IF EXISTS `task_submission`;
CREATE TABLE `task_submission`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT (uuid()),
  `task_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `evidence_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `submission_no`(`submission_no` ASC) USING BTREE,
  INDEX `fk_submission_user`(`user_id` ASC) USING BTREE,
  INDEX `idx_submission_task_user_status`(`task_id` ASC, `user_id` ASC, `status` ASC) USING BTREE,
  CONSTRAINT `fk_submission_task` FOREIGN KEY (`task_id`) REFERENCES `task` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_submission_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '任务提交表：用户提交任务完成证明及审批状态' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of task_submission
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表：存储登录账号及创建/更新时间' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (2, '123', 's.dpgbu@dbmcxsnu.pk', '$2a$10$cvIwjYpgubBMTuxrgSCSOuoK5cFMbx7t.Fg32xCS3xU35wzogrmjm', '2025-12-28 00:49:56', '2025-12-28 00:49:56');

SET FOREIGN_KEY_CHECKS = 1;
