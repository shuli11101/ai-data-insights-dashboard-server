
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
DROP TABLE IF EXISTS `ai_analysis_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_analysis_records` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `analysis_type` varchar(20) NOT NULL COMMENT '分析类型：skill_eval-技能评估；jd_parse-JD拆解；job_read-岗位解读',
  `input_content` text NOT NULL COMMENT '用户输入的原始内容',
  `result` json DEFAULT NULL COMMENT 'AI返回的结构化分析结果',
  `related_job_id` bigint DEFAULT NULL COMMENT '关联岗位ID（单条岗位解读时使用）',
  `visitor_id` varchar(64) DEFAULT '' COMMENT '访客唯一标识（无需登录，浏览器生成）',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '分析创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_visitor` (`visitor_id`),
  KEY `idx_related_job` (`related_job_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI分析历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_conversation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `scene` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `title` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `client_ip` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_conversation` (`user_id`,`conversation_id`),
  KEY `idx_user_update` (`user_id`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=141 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `ai_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `conversation_pk` bigint unsigned NOT NULL,
  `seq` int unsigned NOT NULL,
  `role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_seq` (`conversation_pk`,`seq`)
) ENGINE=InnoDB AUTO_INCREMENT=316 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `app_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `app_user` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `phone` varchar(20) COLLATE utf8mb4_general_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `nickname` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `current_job` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `location` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `category_skill_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_skill_map` (
  `path` varchar(160) NOT NULL,
  `skills` json NOT NULL,
  PRIMARY KEY (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `city`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `city` (
  `id` int NOT NULL,
  `code` bigint DEFAULT NULL COMMENT '行政区划代码',
  `name` varchar(32) DEFAULT NULL COMMENT '名称',
  `province` varchar(32) DEFAULT NULL COMMENT '省/直辖市',
  `city` varchar(32) DEFAULT NULL COMMENT '市',
  `area` varchar(32) DEFAULT NULL COMMENT '区',
  `town` varchar(32) DEFAULT NULL COMMENT '城镇地区',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `city_2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `city_2` (
  `id` int NOT NULL,
  `code` bigint DEFAULT NULL COMMENT '行政区划代码',
  `name` varchar(32) DEFAULT NULL COMMENT '名称',
  `province` varchar(32) DEFAULT NULL COMMENT '省/直辖市',
  `city` varchar(32) DEFAULT NULL COMMENT '市',
  `area` varchar(32) DEFAULT NULL COMMENT '区',
  `town` varchar(32) DEFAULT NULL COMMENT '城镇地区',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `city_backup_20261118`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `city_backup_20261118` (
  `id` int NOT NULL,
  `code` bigint DEFAULT NULL COMMENT '行政区划代码',
  `name` varchar(32) DEFAULT NULL COMMENT '名称',
  `province` varchar(32) DEFAULT NULL COMMENT '省/直辖市',
  `city` varchar(32) DEFAULT NULL COMMENT '市',
  `area` varchar(32) DEFAULT NULL COMMENT '区',
  `town` varchar(32) DEFAULT NULL COMMENT '城镇地区'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_category` (
  `id` int NOT NULL,
  `code` int NOT NULL,
  `l1` varchar(32) NOT NULL,
  `l2` varchar(64) NOT NULL,
  `l3` varchar(128) NOT NULL,
  `path` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_l1` (`l1`),
  KEY `idx_l3` (`l3`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_category_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_category_map` (
  `job_name` varchar(255) NOT NULL,
  `norm_name` varchar(255) NOT NULL,
  `code` int DEFAULT NULL,
  `l1` varchar(32) DEFAULT NULL,
  `l2` varchar(64) DEFAULT NULL,
  `l3` varchar(128) DEFAULT NULL,
  `method` varchar(16) NOT NULL,
  `confidence` decimal(4,2) DEFAULT NULL,
  PRIMARY KEY (`job_name`),
  KEY `idx_code` (`code`),
  KEY `idx_method` (`method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_id` bigint NOT NULL COMMENT '收藏的岗位ID',
  `visitor_id` varchar(64) NOT NULL COMMENT '访客唯一标识',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_job` (`visitor_id`,`job_id`),
  KEY `idx_visitor` (`visitor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_match_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_match_report` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `job_id` bigint unsigned NOT NULL,
  `score` int NOT NULL DEFAULT '0',
  `content` mediumtext COLLATE utf8mb4_general_ci NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_job` (`user_id`,`job_id`),
  KEY `idx_user_time` (`user_id`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_positions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_name` varchar(100) NOT NULL COMMENT '岗位名称',
  `company` varchar(100) NOT NULL COMMENT '公司名称',
  `city` varchar(50) DEFAULT '' COMMENT '城市',
  `district` varchar(50) DEFAULT '' COMMENT '所在行政区',
  `salary_min` int DEFAULT '0' COMMENT '最低薪资（单位：K）',
  `salary_max` int DEFAULT '0' COMMENT '最高薪资（单位：K）',
  `experience` varchar(50) DEFAULT '' COMMENT '经验要求（如：不限经验）',
  `skill_tags` json DEFAULT NULL COMMENT '技能标签数组（JSON格式）',
  `publish_date` date DEFAULT NULL COMMENT '岗位发布日期',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '数据导入时间',
  PRIMARY KEY (`id`),
  KEY `idx_city` (`city`),
  KEY `idx_salary_range` (`salary_min`,`salary_max`),
  KEY `idx_experience` (`experience`)
) ENGINE=InnoDB AUTO_INCREMENT=24015 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息主表';
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_positions_v2`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_positions_v2` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_id` varchar(64) DEFAULT NULL COMMENT '平台职位ID/加密ID',
  `job_name` varchar(100) NOT NULL,
  `company` varchar(100) DEFAULT NULL,
  `city` varchar(50) DEFAULT NULL COMMENT '原文城市',
  `city_code` char(6) DEFAULT NULL COMMENT '统一到 city 表 6 位编码',
  `district` varchar(50) DEFAULT NULL,
  `salary_min` int DEFAULT NULL,
  `salary_max` int DEFAULT NULL,
  `salary_desc` varchar(64) DEFAULT NULL,
  `experience` varchar(50) DEFAULT NULL,
  `degree` varchar(32) DEFAULT NULL,
  `skill_tags` json DEFAULT NULL,
  `skill_std` json DEFAULT NULL,
  `job_attrs` json DEFAULT NULL,
  `publish_date` date DEFAULT NULL,
  `source` varchar(20) NOT NULL DEFAULT 'boss',
  `source_url` varchar(500) DEFAULT NULL,
  `l1` varchar(32) DEFAULT NULL,
  `l2` varchar(64) DEFAULT NULL,
  `l3` varchar(128) DEFAULT NULL,
  `category_code` int DEFAULT NULL,
  `category_method` varchar(16) DEFAULT NULL,
  `crawled_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `security_id` varchar(512) DEFAULT NULL,
  `lid` varchar(64) DEFAULT NULL,
  `skill_source` varchar(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job` (`job_id`),
  KEY `idx_city` (`city_code`),
  KEY `idx_cat` (`l1`,`l2`),
  KEY `idx_pub` (`publish_date`),
  KEY `idx_name` (`job_name`),
  KEY `idx_v2_city_l2` (`city`,`l2`),
  KEY `idx_v2_l2` (`l2`)
) ENGINE=InnoDB AUTO_INCREMENT=5273 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `job_raw`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `job_raw` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `job_name` varchar(500) DEFAULT NULL COMMENT '岗位名称',
  `company` varchar(500) DEFAULT NULL COMMENT '公司名称',
  `city` varchar(200) DEFAULT NULL COMMENT '城市',
  `district` varchar(200) DEFAULT NULL COMMENT '区域',
  `salary_min` varchar(50) DEFAULT NULL COMMENT '原始最低薪资(元，CSV原始数字，先存字符串)',
  `salary_max` varchar(50) DEFAULT NULL COMMENT '原始最高薪资(元)',
  `experience` varchar(100) DEFAULT NULL COMMENT '经验要求',
  `skill_tags` text COMMENT '原始JSON数组字符串，CSV直接读入',
  `publish_date_str` varchar(30) DEFAULT NULL COMMENT '原始日期，只有月日，例如 07‑08，**字符串！不是date**',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24015 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `skill_dict`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `skill_dict` (
  `code` varchar(16) NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `category` varchar(16) NOT NULL,
  `tag_count` int NOT NULL DEFAULT '0',
  `job_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`code`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `tag_skill_map`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag_skill_map` (
  `tag` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `skill_code` varchar(16) NOT NULL,
  `skill_name` varchar(64) NOT NULL,
  `category` varchar(16) NOT NULL,
  `method` varchar(16) NOT NULL DEFAULT 'llm',
  PRIMARY KEY (`tag`),
  KEY `idx_skill` (`skill_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorite` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `job_id` bigint unsigned NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_job` (`user_id`,`job_id`),
  KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `user_preference`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_preference` (
  `user_id` bigint unsigned NOT NULL,
  `city` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `job_title` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `experience` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `education` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `skill_tags` json DEFAULT NULL,
  `current_salary` int DEFAULT NULL,
  `expect_salary` int DEFAULT NULL,
  `job_status` varchar(16) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
DROP TABLE IF EXISTS `v_job_category`;
/*!50001 DROP VIEW IF EXISTS `v_job_category`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `v_job_category` AS SELECT 
 1 AS `id`,
 1 AS `job_name`,
 1 AS `company`,
 1 AS `city`,
 1 AS `district`,
 1 AS `salary_min`,
 1 AS `salary_max`,
 1 AS `experience`,
 1 AS `publish_date`,
 1 AS `l1`,
 1 AS `l2`,
 1 AS `l3`,
 1 AS `code`*/;
SET character_set_client = @saved_cs_client;
/*!50001 DROP VIEW IF EXISTS `v_job_category`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_job_category` AS select `p`.`id` AS `id`,`p`.`job_name` AS `job_name`,`p`.`company` AS `company`,`p`.`city` AS `city`,`p`.`district` AS `district`,`p`.`salary_min` AS `salary_min`,`p`.`salary_max` AS `salary_max`,`p`.`experience` AS `experience`,`p`.`publish_date` AS `publish_date`,`m`.`l1` AS `l1`,`m`.`l2` AS `l2`,`m`.`l3` AS `l3`,`m`.`code` AS `code` from (`job_positions` `p` left join `job_category_map` `m` on((`m`.`job_name` = `p`.`job_name`))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

