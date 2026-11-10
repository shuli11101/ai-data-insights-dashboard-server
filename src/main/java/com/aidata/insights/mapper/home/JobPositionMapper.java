package com.aidata.insights.mapper.home;

import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.vo.home.AreaJobCountVO;
import com.aidata.insights.vo.home.ExperienceSalaryVO;
import com.aidata.insights.vo.home.SalaryRangeVO;
import com.aidata.insights.vo.home.SkillTagVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 岗位 Mapper
 *
 * <p>查询条件全部在 Service 里用 LambdaQueryWrapper 组装，无需 XML；
 * 聚合统计类 SQL 因 MyBatis-Plus 不支持，写在 {@link Select} 注解上。</p>
 */
@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {

    /**
     * 按平均薪资（salary_min / salary_max 的中值）分箱统计岗位数量
     *
     * <p>区间语义为 [下界, 上界)，40K+ 为 >= 40。</p>
     */
    @Select("""
            SELECT CASE
                       WHEN (salary_min + salary_max) / 2 < 10 THEN '0-10K'
                       WHEN (salary_min + salary_max) / 2 < 15 THEN '10-15K'
                       WHEN (salary_min + salary_max) / 2 < 20 THEN '15-20K'
                       WHEN (salary_min + salary_max) / 2 < 25 THEN '20-25K'
                       WHEN (salary_min + salary_max) / 2 < 30 THEN '25-30K'
                       WHEN (salary_min + salary_max) / 2 < 35 THEN '30-35K'
                       WHEN (salary_min + salary_max) / 2 < 40 THEN '35-40K'
                       ELSE '40K+'
                   END AS salary_range,
                   COUNT(*) AS job_count
            FROM job_positions
            GROUP BY salary_range
            ORDER BY MIN((salary_min + salary_max) / 2)
            """)
    List<SalaryRangeVO> countBySalaryRange();

    /**
     * 技术栈热度 Top 10：展开 skill_tags JSON 数组，按标签（小写、去空格）统计提及岗位数
     *
     * <p>历史数据中存在 null 元素，必须显式过滤，否则会被聚合成一条空标签记录。</p>
     */
    @Select("""
            SELECT LOWER(TRIM(t.tag)) AS skill_tag,
                   COUNT(*) AS hot_count
            FROM job_positions j
            JOIN JSON_TABLE(j.skill_tags, '$[*]' COLUMNS (tag VARCHAR(100) PATH '$')) t
            WHERE t.tag IS NOT NULL
              AND TRIM(t.tag) <> ''
            GROUP BY skill_tag
            ORDER BY hot_count DESC, skill_tag
            LIMIT 10
            """)
    List<SkillTagVO> selectHotSkillTags();

    /**
     * 城市岗位数量：岗位数前 8 的城市 + 其余合并为「其他」
     *
     * <p>「异地招聘」不是真实城市（940 条），统计时剔除。</p>
     */
    @Select("""
            WITH city_count AS (
                SELECT city AS area_name,
                       COUNT(*) AS job_count,
                       ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC, city) AS rn
                FROM job_positions
                WHERE city IS NOT NULL
                  AND TRIM(city) <> ''
                  AND city <> '异地招聘'
                GROUP BY city
            )
            SELECT area_name, job_count
            FROM (
                SELECT area_name, job_count, rn AS sort_key
                FROM city_count
                WHERE rn <= 8
                UNION ALL
                SELECT '其他' AS area_name, SUM(job_count) AS job_count, 9999 AS sort_key
                FROM city_count
                WHERE rn > 8
                HAVING COUNT(*) > 0
            ) u
            ORDER BY sort_key
            """)
    List<AreaJobCountVO> selectCityJobCounts();

    /**
     * 按工作经验分档统计平均薪资
     *
     * <p>分档口径：无需经验（含在校生/应届生）、1-3年（1年/2年）、3-5年（3-4年）、
     * ≥5年（其余取值，含 5-7年/8-9年/10年以上）。</p>
     *
     * <p>分档映射只在 CASE 中定义一次，WHERE 仅排除 NULL 与空串：
     * 后续出现新的经验文本会被 ELSE 兜入 ≥5年，不会静默漏统。</p>
     */
    @Select("""
            SELECT experience,
                   ROUND(AVG(avg_salary)) AS avg_salary
            FROM (
                SELECT CASE
                           WHEN experience IN ('无需经验', '在校生/应届生') THEN '无需经验'
                           WHEN experience IN ('1年经验', '2年经验') THEN '1-3年'
                           WHEN experience = '3-4年经验' THEN '3-5年'
                           ELSE '≥5年'
                       END AS experience,
                       (salary_min + salary_max) / 2 AS avg_salary
                FROM job_positions
                WHERE experience IS NOT NULL
                  AND TRIM(experience) <> ''
            ) t
            GROUP BY experience
            ORDER BY FIELD(experience, '无需经验', '1-3年', '3-5年', '≥5年')
            """)
    List<ExperienceSalaryVO> selectExperienceSalary();
}
