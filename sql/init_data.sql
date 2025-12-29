SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO `admin_user` VALUES (1, '123', '$2a$10$cvIwjYpgubBMTuxrgSCSOuoK5cFMbx7t.Fg32xCS3xU35wzogrmjm', '2025-12-28 00:48:56', '2025-12-28 00:53:19');

INSERT INTO `category` VALUES (1, '实体周边');
INSERT INTO `category` VALUES (2, '虚拟兑换');

INSERT INTO `point_account` VALUES (2, 2, 50, '2025-12-29 16:01:55');

INSERT INTO `pool` VALUES (1, '1e90f2e7-6a3f-46a4-8769-d879c1acf26d', '常驻卡池', '是常驻卡池！', '2025-12-26 05:46:25', '2029-05-31 10:17:09', 'ON', 12, 'TYPE', '2025-12-29 13:24:44', '2025-12-29 16:54:32');

INSERT INTO `pool_item` VALUES (3, 1, 3, 5, 1);
INSERT INTO `pool_item` VALUES (4, 1, 4, 2, 1);

INSERT INTO `reward` VALUES (3, '徽章', '53c3fe7b-978c-4913-b7e9-0ca09dc7a6f5', 'IPP的徽章！', 20, 'ON', '2025-12-29 11:39:33', '2025-12-29 16:02:23', 0);
INSERT INTO `reward` VALUES (4, '钥匙扣', '5b4b2c62-4309-490a-bec4-e36d7453b757', '一个好看的钥匙扣', 20, 'ON', '2025-12-29 13:39:23', '2025-12-29 16:02:42', 0);

INSERT INTO `reward_category` VALUES (1, 3, 1);
INSERT INTO `reward_category` VALUES (2, 4, 1);

INSERT INTO `reward_inventory` VALUES (3, 20, 3, '2025-12-29 11:56:43');
INSERT INTO `reward_inventory` VALUES (4, 20, 0, '2025-12-29 13:39:23');

INSERT INTO `users` VALUES (2, '123', 's.dpgbu@dbmcxsnu.pk', '$2a$10$cvIwjYpgubBMTuxrgSCSOuoK5cFMbx7t.Fg32xCS3xU35wzogrmjm', '2025-12-28 00:49:56', '2025-12-28 00:49:56');


SET FOREIGN_KEY_CHECKS = 1;

