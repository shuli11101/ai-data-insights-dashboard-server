package com.aidata.insights.vo.ai;

import lombok.Data;

import java.util.List;

/**
 * AI 行情注入快照：只含与用户无关的统计结果，按「城市 + 方向」缓存
 *
 * <p>用户维度的信息（已掌握技能、期望薪资、职级）不进这里，避免 A 用户的个人情况
 * 被缓存后拼进 B 用户的提示词。</p>
 *
 * <p>本类会被 Jackson 序列化进 Redis 再读回来，保持普通 POJO 即可，不要改成 record。</p>
 */
@Data
public class AiMarketSnapshot {

    /** 城市 + 方向，样本达标 */
    public static final String LEVEL_CITY_DIRECTION = "CITY_DIRECTION";

    /** 全国 + 方向，本地样本不足 */
    public static final String LEVEL_NATION_DIRECTION = "NATION_DIRECTION";

    /** 仅城市，方向未识别 */
    public static final String LEVEL_CITY_ONLY = "CITY_ONLY";

    /** 无可用数据，不注入数字 */
    public static final String LEVEL_NONE = "NONE";

    /**
     * 命中的兜底口径，取值见本类常量：
     * CITY_DIRECTION 城市+方向、NATION_DIRECTION 全国+方向、
     * CITY_ONLY 仅城市、NONE 无可用数据
     *
     * <p>刻意不使用 L1/L2/L3/L4 这种编号，避免与表里的 l1/l2/l3 分类字段混淆。</p>
     */
    private String level;

    /**
     * 命中的城市，用户没提城市时为空
     */
    private String city;

    /**
     * 命中的二级分类，L3 / L4 可能为空
     */
    private String category;

    /**
     * 命中口径的岗位总数，L4 为 0
     */
    private Long sampleSize;

    /**
     * 该城市不限方向的岗位总数，仅 L2 使用：用于说明本地样本不足
     */
    private Long citySampleSize;

    /**
     * 薪资四分位原始结果，样本不足或 L4 时为空
     */
    private List<AiSalaryQuartileVO> salaryQuartiles;

    /**
     * 热门技能，L3 时为该城市整体口径
     */
    private List<AiSkillHeatVO> skills;

    /**
     * 城市岗位结构，仅 L3 使用
     */
    private List<AiCategoryCountVO> categories;
}