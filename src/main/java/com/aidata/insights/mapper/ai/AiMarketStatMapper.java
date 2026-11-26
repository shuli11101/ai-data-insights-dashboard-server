package com.aidata.insights.mapper.ai;

import com.aidata.insights.vo.ai.AiCategoryCountVO;
import com.aidata.insights.vo.ai.AiSalaryQuartileVO;
import com.aidata.insights.vo.ai.AiSkillHeatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 行情数据聚合 Mapper：只读 job_positions_v2，供 AI 提示词注入使用
 *
 * <p>city / l2 传 null 表示该维度不限，由调用方按四级兜底决定传哪些条件。</p>
 *
 * <p>表里 publish_date 全为空，因此只做横截面统计，不提供任何时间序列，
 * 调用方也不应产出「趋势」类表述。</p>
 *
 * <p>可空参数用动态 SQL 拼接：条件不传时不生成对应片段，让 (city, l2) 索引可用。
 * 若写成「参数 IS NULL OR 列 = 参数」，优化器会退化为全表扫，数据量涨到几十万后会明显变慢。</p>
 */
@Mapper
public interface AiMarketStatMapper {

    /**
     * 城市清单：库内实际存在岗位的城市
     *
     * <p>抽词词典用它而不是城市字典表，保证「识别到城市」就一定查得到数据；
     * 「异地招聘」不是真实城市，必须排除。</p>
     */
    @Select("""
            SELECT DISTINCT city
            FROM job_positions_v2
            WHERE city IS NOT NULL
              AND TRIM(city) <> ''
              AND city <> '异地招聘'
            """)
    List<String> selectCities();

    /**
     * 岗位总数，用于判断样本量是否达到阈值
     *
     * <p>这里统计的是岗位总数而不是薪资样本数，因为「该方向有多少条岗位」才是
     * 业务上判断样本是否充足的口径。</p>
     */
    @Select("""
            <script>
            SELECT COUNT(*)
            FROM job_positions_v2
            <where>
                <if test="city != null">AND city = #{city}</if>
                <if test="l2 != null">AND l2 = #{l2}</if>
            </where>
            </script>
            """)
    long countJobs(@Param("city") String city, @Param("l2") String l2);

    /**
     * 薪资四分位分箱：按薪资中值 NTILE(4) -> 将结果切成4份
     *
     * <p>排除实习与应届，避免 4K 左右的实习岗位把分位数整体拉低；
     * 薪资为 NULL 的岗位也排除，否则会被归入最低箱。</p>
     *
     * <p>返回 1~4 行，每行给出该箱的岗位数、下限、均值、上限。
     * 分位数由调用方用相邻箱边界的中点近似，MySQL 没有 PERCENTILE_CONT。</p>
     */
    @Select("""
            <script>
            WITH salary AS (
                SELECT (salary_min + salary_max) / 2 AS mid
                FROM job_positions_v2
                WHERE salary_min IS NOT NULL
                  AND salary_max IS NOT NULL
                  AND experience NOT IN ('实习', '在校/应届')
                  AND job_name NOT LIKE '%实习%'
                  <if test="city != null">AND city = #{city}</if>
                  <if test="l2 != null">AND l2 = #{l2}</if>
            ), grouped AS (
                SELECT mid, NTILE(4) OVER (ORDER BY mid) AS quartile FROM salary
            )
            SELECT quartile,
                   COUNT(*) AS sample_count,
                   MIN(mid) AS min_mid,
                   ROUND(AVG(mid), 1) AS avg_mid,
                   MAX(mid) AS max_mid
            FROM grouped
            GROUP BY quartile
            ORDER BY quartile
            </script>
            """)
    List<AiSalaryQuartileVO> selectSalaryQuartiles(@Param("city") String city,
                                                   @Param("l2") String l2);

    /**
     * 技能热度 Top N
     *
     * <p>skill_tags 是抓取时把 JD 标签整体塞进来的 JSON 数组，混有
     * 「数学/统计相关专业」「抖音」「一对一教学」「销售岗」这类非技能内容，
     * 因此做了三层过滤：关键词黑名单、长度上限 12、空标签剔除。</p>
     *
     * <p>黑名单分两类：一类是招聘文案噪声（经验、专业、能力、教学、搭建），
     * 一类是电商/零售类的平台名与品类名（天猫、淘宝、服饰、美妆、日用、直播）。
     * 后者主要用于挡掉运营方向里大量「爆品运营」「店铺站内推广」这类工作描述。</p>
     */
    @Select("""
            <script>
            SELECT LOWER(TRIM(t.tag)) AS skill_tag,
                   COUNT(*) AS hot_count
            FROM job_positions_v2 j
            JOIN JSON_TABLE(j.skill_tags, '$[*]' COLUMNS (tag VARCHAR(100) PATH '$')) t
            WHERE TRIM(t.tag) != ''
              AND CHAR_LENGTH(TRIM(t.tag)) &lt;= 12
              AND TRIM(t.tag) NOT REGEXP '经验|专业|能力|掌握|办公|教学|培训|销售|客服|岗|搭建|面销|陌拜|电话|一对一|小班|店铺|品类|站内|站外|策划|直播|爆品|抖音|淘宝|天猫|拼多多|小红书|京东|1688|服饰|美妆|日用|策略|模式'
              <if test="city != null">AND j.city = #{city}</if>
              <if test="l2 != null">AND j.l2 = #{l2}</if>
            GROUP BY skill_tag
            ORDER BY hot_count DESC, skill_tag
            LIMIT #{limit}
            </script>
            """)
    List<AiSkillHeatVO> selectSkillHeat(@Param("city") String city,
                                        @Param("l2") String l2,
                                        @Param("limit") int limit);

    /**
     * 技能热度 Top N（基于 skill_dict / tag_skill_map）
     *
     * <p>相比 {@link #selectSkillHeat}：只要标签在 tag_skill_map 里有映射就计入，
     * 不需要维护黑名单正则；同时把「MySQL数据库」「熟悉MySQL」这类写法归一到同一技能，
     * 并把「小班教学」「抖音」这类噪声直接排除（它们不在映射表里）。</p>
     *
     * <p>tag_skill_map.tag 存的是原始写法，依赖表的 ai_ci 排序规则做大小写不敏感匹配。</p>
     */
    @Select("""
            <script>
            SELECT s.name AS skill_tag,
                   COUNT(*) AS hot_count
            FROM job_positions_v2 j
            JOIN JSON_TABLE(j.skill_tags, '$[*]' COLUMNS (tag VARCHAR(100) PATH '$')) t
            JOIN tag_skill_map m ON m.tag = TRIM(t.tag)
            JOIN skill_dict s ON s.code = m.skill_code
            WHERE TRIM(t.tag) != ''
              <if test="city != null">AND j.city = #{city}</if>
              <if test="l2 != null">AND j.l2 = #{l2}</if>
            GROUP BY s.code, s.name
            ORDER BY hot_count DESC, s.name
            LIMIT #{limit}
            </script>
            """)
    List<AiSkillHeatVO> selectSkillHeatFromDict(@Param("city") String city,
                                                @Param("l2") String l2,
                                                @Param("limit") int limit);

    /**
     * 城市岗位结构（「仅城市」兜底）：按二级分类计数
     *
     * <p>用户只给了城市、没给出可识别的岗位方向时使用：不再按 l2 过滤，
     * 而是把该城市的岗位按 l2 分组，说明这个城市主要招什么方向。</p>
     */
    @Select("""
            SELECT l2 AS category,
                   COUNT(*) AS job_count
            FROM job_positions_v2
            WHERE city = #{city}
              AND l2 IS NOT NULL
            GROUP BY l2
            ORDER BY job_count DESC
            LIMIT #{limit}
            """)
    List<AiCategoryCountVO> selectCityCategories(@Param("city") String city,
                                                 @Param("limit") int limit);
}