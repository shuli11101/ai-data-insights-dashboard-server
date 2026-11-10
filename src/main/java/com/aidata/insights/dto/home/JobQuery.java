package com.aidata.insights.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位列表查询条件
 */
@Data
@Schema(description = "岗位列表查询条件")
public class JobQuery {

    @Schema(description = "页码，从 1 开始", example = "1")
    private long page = 1;

    @Schema(description = "每页条数", example = "10")
    private long size = 10;

    @Schema(description = "城市，如 广州", example = "广州")
    private String city;

    @Schema(description = "经验下限（年）", example = "1")
    private Integer experienceMin;

    @Schema(description = "经验上限（年）", example = "3")
    private Integer experienceMax;

    @Schema(description = "薪资下限（K）", example = "10")
    private Integer salaryMin;

    @Schema(description = "薪资上限（K）", example = "20")
    private Integer salaryMax;

    @Schema(description = "关键词，按岗位名模糊匹配", example = "数据分析")
    private String keyword;

    @Schema(description = "岗位名称，模糊匹配", example = "数据分析师")
    private String jobName;
}
