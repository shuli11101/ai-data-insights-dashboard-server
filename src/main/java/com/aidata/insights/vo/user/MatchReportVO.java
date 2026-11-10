package com.aidata.insights.vo.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析报告列表项
 *
 * <p>字段按前端契约输出驼峰；技能覆盖率与薪资偏差从报告正文中解析。</p>
 */
@Data
@Schema(description = "分析报告列表项")
public class MatchReportVO {

    @Schema(description = "报告 ID", example = "23")
    private Long reportId;

    @Schema(description = "岗位 ID", example = "21079")
    private Long jobId;

    @Schema(description = "岗位名称", example = "Web前端开发工程师")
    private String jobName;

    @Schema(description = "公司名称", example = "广州汉光电气股份有限公司")
    private String company;

    @Schema(description = "工作地点，格式 城市·区县", example = "广州·荔湾区")
    private String location;

    @Schema(description = "经验要求", example = "1年经验")
    private String experience;

    @Schema(description = "技能覆盖率", example = "0")
    private Integer skillCoverage;

    @Schema(description = "薪资偏差百分比，报告未给出时为 null", example = "null")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Integer salaryDeviation;

    @Schema(description = "综合匹配分", example = "18")
    private Integer score;

    @Schema(description = "保存时间", example = "2026-11-07 21:17:18")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime saveTime;

    @Schema(description = "报告正文（Markdown）")
    private String content;
}
