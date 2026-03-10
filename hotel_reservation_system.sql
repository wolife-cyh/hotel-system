/*
 Navicat Premium Dump SQL

 Source Server         : mysql8.0
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : hotel_reservation_system

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 04/08/2025 07:18:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for banner
-- ----------------------------
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轮播图ID',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '轮播图标题',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序(数字越小越靠前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '轮播图表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of banner
-- ----------------------------
INSERT INTO `banner` VALUES (1, '精品客房8折优惠', '/img/1748674884890.png', 1, 1, '2025-05-31 14:53:28', '2025-05-31 14:53:28');
INSERT INTO `banner` VALUES (2, '豪华套房体验', '/img/1748674929696.png', 2, 1, '2025-05-31 14:53:28', '2025-05-31 14:53:28');
INSERT INTO `banner` VALUES (3, '家庭出游首选', '/img/1748674972067.png', 3, 1, '2025-05-31 14:53:28', '2025-05-31 14:53:28');

-- ----------------------------
-- Table structure for orders
-- ----------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `reservation_id` bigint NOT NULL COMMENT '预约ID',
  `amount` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态（0-未支付，1-已支付，2-已取消，3-已退款）',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `pay_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式',
  `pay_no` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付流水号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  UNIQUE INDEX `uk_reservation_id`(`reservation_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_order_reservation_id` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_order_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of orders
-- ----------------------------
INSERT INTO `orders` VALUES (1, '20250531085058646f0d8a', 2, 1, 199.00, 1, '2025-05-31 08:51:44', '支付宝', '', '2025-05-31 08:50:59', '2025-05-31 08:51:44');
INSERT INTO `orders` VALUES (2, '2025053115435947dba877', 2, 3, 199.00, 2, NULL, NULL, NULL, '2025-05-31 15:44:00', '2025-05-31 15:44:07');
INSERT INTO `orders` VALUES (3, '202505311545428030a963', 2, 6, 2097.00, 3, '2025-05-31 15:45:53', '支付宝', '', '2025-05-31 15:45:42', '2025-05-31 16:06:56');
INSERT INTO `orders` VALUES (4, '20250531155153de7dad4d', 2, 7, 699.00, 1, '2025-05-31 15:52:05', '银行卡', '', '2025-05-31 15:51:53', '2025-05-31 15:52:05');
INSERT INTO `orders` VALUES (5, '202505311604535795ca7e', 2, 8, 599.00, 1, '2025-05-31 16:05:02', '银行卡', '', '2025-05-31 16:04:53', '2025-05-31 16:05:02');
INSERT INTO `orders` VALUES (6, '202505311605454effa0ff', 2, 9, 599.00, 1, '2025-05-31 16:05:48', '微信支付', '', '2025-05-31 16:05:45', '2025-05-31 16:05:48');
INSERT INTO `orders` VALUES (7, '20250531170822910d6f25', 2, 10, 199.00, 1, '2025-05-31 17:08:26', '微信支付', '', '2025-05-31 17:08:22', '2025-05-31 17:08:26');
INSERT INTO `orders` VALUES (8, '2025053117130750084db8', 2, 11, 199.00, 1, '2025-05-31 17:13:11', '微信支付', '', '2025-05-31 17:13:07', '2025-05-31 17:13:11');
INSERT INTO `orders` VALUES (9, '20250802085538fcfd422b', 2, 12, 9867.00, 1, '2025-08-02 08:55:49', '微信支付', '', '2025-08-02 08:55:39', '2025-08-02 08:55:49');
INSERT INTO `orders` VALUES (10, '20250802204851f4cd0916', 2, 13, 9751.00, 3, '2025-08-02 20:48:59', '微信支付', '', '2025-08-02 20:48:52', '2025-08-02 20:50:49');
INSERT INTO `orders` VALUES (11, '20250802205515cd475476', 2, 14, 12259.00, 1, '2025-08-02 20:55:22', '支付宝', '', '2025-08-02 20:55:15', '2025-08-02 20:55:22');
INSERT INTO `orders` VALUES (12, '2025080220554108efcf99', 2, 15, 599.00, 1, '2025-08-02 20:55:50', '银行卡', '', '2025-08-02 20:55:42', '2025-08-02 20:55:50');

-- ----------------------------
-- Table structure for reservation
-- ----------------------------
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `start_date` date NOT NULL COMMENT '入住日期',
  `end_date` date NOT NULL COMMENT '退房日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态（0-待确认，1-已确认，2-已取消，3-已完成）',
  `price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '预约总价',
  `pay_status` tinyint NOT NULL DEFAULT 0 COMMENT '支付状态（0-未支付，1-已支付，2-已退款）',
  `guest_count` int NOT NULL DEFAULT 1 COMMENT '入住人数',
  `guest_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入住人姓名',
  `guest_phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入住人电话',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '备注信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`room_id` ASC) USING BTREE,
  INDEX `idx_date`(`start_date` ASC, `end_date` ASC) USING BTREE,
  CONSTRAINT `fk_reservation_room_id` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_reservation_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 16 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of reservation
-- ----------------------------
INSERT INTO `reservation` VALUES (1, 2, 1, '2025-06-04', '2025-06-05', 3, 199.00, 1, 1, '测试用户', '13900139000', '', '2025-05-31 08:50:58', '2025-05-31 08:51:44');
INSERT INTO `reservation` VALUES (2, 2, 1, '2025-05-31', '2025-06-01', 1, 199.00, 0, 1, '测试用户', '13900139000', '', '2025-05-31 10:24:34', '2025-05-31 10:24:34');
INSERT INTO `reservation` VALUES (3, 2, 2, '2025-05-31', '2025-06-01', 2, 199.00, 2, 1, '测试用户', '13900139000', '', '2025-05-31 15:43:36', '2025-05-31 15:44:07');
INSERT INTO `reservation` VALUES (4, 2, 2, '2025-05-31', '2025-06-01', 0, 199.00, 0, 1, '测试用户', '13900139000', '', '2025-05-31 15:44:17', '2025-05-31 15:44:17');
INSERT INTO `reservation` VALUES (5, 2, 3, '2025-05-31', '2025-06-01', 1, 299.00, 0, 1, '测试用户', '13900139000', '', '2025-05-31 15:44:26', '2025-05-31 15:44:26');
INSERT INTO `reservation` VALUES (6, 2, 11, '2025-06-08', '2025-06-11', 3, 2097.00, 1, 1, '测试用户', '13900139000', '', '2025-05-31 15:45:41', '2025-05-31 16:06:56');
INSERT INTO `reservation` VALUES (7, 2, 11, '2025-05-31', '2025-06-01', 1, 699.00, 1, 1, '测试用户', '13900139000', '', '2025-05-31 15:50:13', '2025-05-31 15:52:05');
INSERT INTO `reservation` VALUES (8, 2, 9, '2025-05-31', '2025-06-01', 1, 599.00, 1, 1, '测试用户', '13900139000', 'OKOKOKOKOK', '2025-05-31 16:04:47', '2025-05-31 16:05:02');
INSERT INTO `reservation` VALUES (9, 2, 10, '2025-05-31', '2025-06-01', 3, 599.00, 1, 1, '测试用户', '13900139000', '', '2025-05-31 16:05:37', '2025-05-31 16:05:48');
INSERT INTO `reservation` VALUES (10, 2, 5, '2025-05-31', '2025-06-01', 2, 199.00, 2, 1, '测试用户', '13900139000', '', '2025-05-31 17:08:16', '2025-05-31 17:08:26');
INSERT INTO `reservation` VALUES (11, 2, 6, '2025-05-31', '2025-06-01', 1, 199.00, 1, 1, '测试用户', '13900139000', '', '2025-05-31 17:13:03', '2025-05-31 17:13:11');
INSERT INTO `reservation` VALUES (12, 2, 3, '2025-08-23', '2025-09-25', 3, 9867.00, 1, 2, '测试用户', '13900139000', '1221212212', '2025-08-02 08:55:38', '2025-08-02 08:55:49');
INSERT INTO `reservation` VALUES (13, 2, 1, '2025-08-09', '2025-09-27', 3, 9751.00, 1, 1, '张三', '13800138001', '111', '2025-08-02 20:48:51', '2025-08-02 20:50:49');
INSERT INTO `reservation` VALUES (14, 2, 3, '2025-08-02', '2025-09-12', 0, 12259.00, 1, 1, '张三', '13800138001', '', '2025-08-02 20:55:14', '2025-08-02 20:55:22');
INSERT INTO `reservation` VALUES (15, 2, 9, '2025-08-02', '2025-08-03', 0, 599.00, 1, 1, '张三', '13800138001', '', '2025-08-02 20:55:33', '2025-08-02 20:55:50');

-- ----------------------------
-- Table structure for review
-- ----------------------------
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评价ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `reservation_id` bigint NOT NULL COMMENT '预约ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评价内容',
  `score` tinyint NOT NULL DEFAULT 5 COMMENT '评分（1-5）',
  `images` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '评价图片URL，多个以逗号分隔',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '回复内容',
  `reply_time` datetime NULL DEFAULT NULL COMMENT '回复时间',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0-隐藏，1-显示）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_reservation_id`(`reservation_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_room_id`(`room_id` ASC) USING BTREE,
  CONSTRAINT `fk_review_reservation_id` FOREIGN KEY (`reservation_id`) REFERENCES `reservation` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_room_id` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评价表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of review
-- ----------------------------
INSERT INTO `review` VALUES (1, 2, 1, 1, 'OKOKOK', 5, '/img/1748677105381.png', '谢谢！', '2025-05-31 16:31:53', 0, '2025-05-31 15:38:32', '2025-05-31 16:32:00');
INSERT INTO `review` VALUES (2, 2, 3, 12, '棒是1111', 5, '', '谢谢', '2025-08-02 20:51:04', 1, '2025-08-02 20:49:47', '2025-08-02 20:51:04');

-- ----------------------------
-- Table structure for room
-- ----------------------------
DROP TABLE IF EXISTS `room`;
CREATE TABLE `room`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `room_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房间号',
  `room_type_id` bigint NOT NULL COMMENT '房间类型ID',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态（0-维护中，1-可用）',
  `floor` int NOT NULL DEFAULT 1 COMMENT '楼层',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '房间描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_room_number`(`room_number` ASC) USING BTREE,
  INDEX `idx_room_type_id`(`room_type_id` ASC) USING BTREE,
  CONSTRAINT `fk_room_type_id` FOREIGN KEY (`room_type_id`) REFERENCES `room_type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of room
-- ----------------------------
INSERT INTO `room` VALUES (1, '101', 1, 1, 1, '一楼靠近电梯，安静舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (2, '102', 1, 1, 1, '一楼靠近电梯，安静舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (3, '103', 2, 1, 1, '一楼靠近电梯，视野开阔', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (4, '104', 2, 1, 1, '一楼靠近电梯，视野开阔', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (5, '201', 1, 1, 2, '二楼靠近电梯，安静舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (6, '202', 1, 1, 2, '二楼靠近电梯，安静舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (7, '203', 2, 1, 2, '二楼靠近电梯，视野开阔', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (8, '204', 2, 1, 2, '二楼靠近电梯，视野开阔', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (9, '301', 3, 1, 3, '三楼豪华套房，视野极佳', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (10, '302', 3, 1, 3, '三楼豪华套房，视野极佳', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (11, '401', 4, 1, 4, '四楼家庭套房，宽敞舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');
INSERT INTO `room` VALUES (12, '402', 4, 1, 4, '四楼家庭套房，宽敞舒适', '2025-05-31 04:09:05', '2025-05-31 04:09:05');

-- ----------------------------
-- Table structure for room_type
-- ----------------------------
DROP TABLE IF EXISTS `room_type`;
CREATE TABLE `room_type`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '房型ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '房型名称',
  `price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '房型价格',
  `max_people` int NOT NULL DEFAULT 1 COMMENT '最大入住人数',
  `bed_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '床型描述',
  `facilities` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '设施描述',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '房型详细描述',
  `image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '房型图片URL',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间类型表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of room_type
-- ----------------------------
INSERT INTO `room_type` VALUES (1, '标准单人间', 199.00, 1, '单人床1.2m', '空调、电视、WiFi、独立卫浴', '舒适的单人房间，配备所有基本设施，适合商务出行。', '/img/1748658375559.jpg', '2025-05-31 04:09:04', '2025-05-31 10:27:45');
INSERT INTO `room_type` VALUES (2, '标准双人间', 299.00, 2, '双人床1.8m', '空调、电视、WiFi、独立卫浴', '温馨的双人房间，配备舒适的双人床，适合情侣或朋友入住。', '/img/1748658601370.jpg', '2025-05-31 04:09:04', '2025-05-31 10:30:10');
INSERT INTO `room_type` VALUES (3, '豪华套房', 599.00, 3, '大床2.0m+单人沙发床', '空调、液晶电视、WiFi、独立卫浴、迷你吧、休息区', '宽敞的豪华套房，设有独立的卧室和休息区，提供高品质的住宿体验。', '/img/1748658663735.jpg', '2025-05-31 04:09:04', '2025-05-31 10:31:11');
INSERT INTO `room_type` VALUES (4, '家庭套房', 699.00, 4, '大床2.0m+双人沙发床', '空调、液晶电视、WiFi、独立卫浴、迷你吧、客厅', '专为家庭设计的宽敞套房，配备独立的卧室和客厅，满足家庭出行需求。', '/img/1748658728820.jpg', '2025-05-31 04:09:04', '2025-05-31 10:32:28');

-- ----------------------------
-- Table structure for room_type_image
-- ----------------------------
DROP TABLE IF EXISTS `room_type_image`;
CREATE TABLE `room_type_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图片ID',
  `room_type_id` bigint NOT NULL COMMENT '房间类型ID',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `is_main` tinyint NOT NULL DEFAULT 0 COMMENT '是否主图(0-否,1-是)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_room_type_id`(`room_type_id` ASC) USING BTREE,
  CONSTRAINT `fk_room_type_image_room_type_id` FOREIGN KEY (`room_type_id`) REFERENCES `room_type` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '房间类型图片表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of room_type_image
-- ----------------------------
INSERT INTO `room_type_image` VALUES (1, 1, '/img/1748658457445.jpg', 0, 1, '2025-05-31 10:27:37', '2025-05-31 10:27:37');
INSERT INTO `room_type_image` VALUES (2, 1, '/img/1748658463667.jpg', 1, 0, '2025-05-31 10:27:43', '2025-05-31 10:27:43');
INSERT INTO `room_type_image` VALUES (3, 2, '/img/1748658606383.jpg', 0, 1, '2025-05-31 10:30:06', '2025-05-31 10:30:06');
INSERT INTO `room_type_image` VALUES (4, 2, '/img/1748658608825.jpg', 1, 0, '2025-05-31 10:30:09', '2025-05-31 10:30:09');
INSERT INTO `room_type_image` VALUES (5, 3, '/img/1748658666013.jpg', 0, 1, '2025-05-31 10:31:06', '2025-05-31 10:31:06');
INSERT INTO `room_type_image` VALUES (6, 3, '/img/1748658669214.jpg', 1, 0, '2025-05-31 10:31:09', '2025-05-31 10:31:09');
INSERT INTO `room_type_image` VALUES (7, 4, '/img/1748658730890.jpg', 0, 1, '2025-05-31 10:32:11', '2025-05-31 10:32:11');
INSERT INTO `room_type_image` VALUES (8, 4, '/img/1748658738516.jpg', 1, 0, '2025-05-31 10:32:18', '2025-05-31 10:32:18');
INSERT INTO `room_type_image` VALUES (9, 4, '/img/1748658745850.jpg', 2, 0, '2025-05-31 10:32:26', '2025-05-31 10:32:26');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码(加密存储)',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `role_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'USER' COMMENT '角色code',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '姓名',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态(0:禁用,1:正常)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `sex` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '性别',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$Cc6gX7Jel5UtKreBdrY8SeRiqwqEuccdySNafjQRMtz30KE92sPaS', 'admin@example.com', '13800138000', 'ADMIN', '系统管理员', '/img/1748682721011.jpg', 1, '2025-05-31 04:09:04', '2025-05-31 17:12:01', '女');
INSERT INTO `user` VALUES (2, 'test', '$2a$10$iul6jocLsH.A4gN1QUpgDexDq6KO89syHjUkRD3NbA1L6CTVrNRMO', 'test@example.com', '13800138001', 'USER', '张三', '/img/1754125965274.jpg', 1, '2025-05-31 04:09:04', '2025-08-02 17:12:53', '男');

SET FOREIGN_KEY_CHECKS = 1;
