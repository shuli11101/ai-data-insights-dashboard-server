-- ============================================================
-- 生产库增量变更脚本
--
-- 适用前提：库中已存在 job_positions_v2 / ai_conversation 等基础表
--           本脚本只做「行情数据注入」相关的新增，不建业务基础表
--
-- 执行方式：
--   mysql -h <RDS内网地址> -u <用户> -p recruitment_dashboard < 01_schema.sql
--   或在 RDS 控制台的 SQL 窗口整段执行
--
-- 可重复执行：索引和建表都做了存在性判断，重复跑不会报错
-- ============================================================

-- ------------------------------------------------------------
-- 1. 行情注入查询所需索引
--    city 和 l2 原本没有单独索引，只有 city_code 和 (l1, l2)
--    不加索引时 countJobs 会全表扫，数据涨到几十万后明显变慢
-- ------------------------------------------------------------
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'job_positions_v2'
                 AND index_name = 'idx_v2_city_l2');
SET @ddl := IF(@exist = 0,
    'CREATE INDEX idx_v2_city_l2 ON job_positions_v2 (city, l2)',
    'SELECT ''idx_v2_city_l2 已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE()
                 AND table_name = 'job_positions_v2'
                 AND index_name = 'idx_v2_l2');
SET @ddl := IF(@exist = 0,
    'CREATE INDEX idx_v2_l2 ON job_positions_v2 (l2)',
    'SELECT ''idx_v2_l2 已存在，跳过'' AS msg');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2. 技能字典
--    code 为业务编码，name 为标准展示名，category 用于分类筛选
--    tag_count / job_count 为冗余统计，由数据侧回填
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS skill_dict (
    code      VARCHAR(16) NOT NULL COMMENT '技能编码，如 S0001',
    name      VARCHAR(64) NOT NULL COMMENT '标准展示名，如 Java / Power BI',
    category  VARCHAR(16) NOT NULL COMMENT 'lang/db/framework/tool/business/soft/cert',
    tag_count INT         NOT NULL DEFAULT 0 COMMENT '映射到该技能的原始标签数',
    job_count INT         NOT NULL DEFAULT 0 COMMENT '引用该技能的岗位数',
    PRIMARY KEY (code),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='技能标准字典';

-- ------------------------------------------------------------
-- 3. 原始标签 → 标准技能的映射
--    tag 存的是 skill_tags 里的原始写法（大小写要保持一致，
--    查询侧依赖 ai_ci 排序规则做大小写不敏感匹配）
--    method 记录来源：llm / rule / alias / manual
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tag_skill_map (
    tag        VARCHAR(128) NOT NULL COMMENT '原始标签写法',
    skill_code VARCHAR(16)  NOT NULL COMMENT '对应 skill_dict.code',
    skill_name VARCHAR(64)  NOT NULL COMMENT '冗余，便于人工核对',
    category   VARCHAR(16)  NOT NULL COMMENT '冗余，便于人工核对',
    method     VARCHAR(16)  NOT NULL DEFAULT 'llm' COMMENT 'llm/rule/alias/manual',
    PRIMARY KEY (tag),
    KEY idx_skill (skill_code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT ='原始技能标签到标准技能的映射';

-- ------------------------------------------------------------
-- 4. 校验
-- ------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'job_positions_v2'
       AND index_name IN ('idx_v2_city_l2', 'idx_v2_l2')) AS index_count,
    (SELECT COUNT(*) FROM skill_dict)    AS skill_dict_rows,
    (SELECT COUNT(*) FROM tag_skill_map) AS tag_skill_map_rows;
-- 期望：index_count = 2；skill_dict_rows / tag_skill_map_rows 在导入
-- 02_skill_seed.sql 之前可能为 0，属正常