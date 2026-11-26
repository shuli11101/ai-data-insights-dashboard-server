package com.aidata.insights.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 薪资四分位分箱，供 AI 行情数据注入使用
 *
 * <p>按 (salary_min + salary_max) / 2 排序后用 NTILE(4) 分箱，quartile 1~4 依次为低到高。
 * 样本少于 4 条时 NTILE 会把数据集中到第 1 箱，调用方需先用岗位总数判断样本是否充足。</p>
 */
@Data
@Schema(description = "薪资四分位分箱")
public class AiSalaryQuartileVO {

    /**
     * 分箱序号：1 为最低的四分之一
     */
    @Schema(description = "分箱序号，1 为最低四分之一", example = "2")
    private Integer quartile;

    /**
     * 该箱纳入统计的岗位数（已排除实习、应届与薪资为空的岗位）
     */
    @Schema(description = "该箱岗位数", example = "42")
    private Long sampleCount;

    /**
     * 该箱薪资下限（K）
     *
     * <p>薪资用 Double 承接：MySQL 的 (salary_min + salary_max) / 2 走除法运算，返回的是 DECIMAL。</p>
     */
    @Schema(description = "该箱薪资下限（K）", example = "6.0")
    private Double minMid;

    /**
     * 该箱平均薪资（K）
     */
    @Schema(description = "该箱平均薪资（K）", example = "9.1")
    private Double avgMid;

    /**
     * 该箱薪资上限（K）
     */
    @Schema(description = "该箱薪资上限（K）", example = "10.5")
    private Double maxMid;
}