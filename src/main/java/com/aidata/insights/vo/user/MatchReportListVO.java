package com.aidata.insights.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分析报告列表结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分析报告列表结果")
public class MatchReportListVO {

    @Schema(description = "分析报告列表")
    private List<MatchReportVO> list;

    @Schema(description = "报告总数", example = "4")
    private long total;
}
