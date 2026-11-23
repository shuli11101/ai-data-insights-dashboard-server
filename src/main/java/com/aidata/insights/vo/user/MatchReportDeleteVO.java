package com.aidata.insights.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量删除分析报告结果
 *
 * <p>不属于当前用户或本就未保存的岗位 ID 计入 missing。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量删除分析报告结果")
public class MatchReportDeleteVO {

    @Schema(description = "实际删除条数", example = "1")
    private int deleted;

    @Schema(description = "未删到的岗位 ID")
    private List<Long> missing;
}