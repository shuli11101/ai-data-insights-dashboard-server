package com.aidata.insights.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量删除分析报告请求
 */
@Data
@Schema(description = "批量删除分析报告请求")
public class DeleteMatchReportRequest {

    @Schema(description = "岗位 ID 列表，单次最多 50 个")
    private List<Long> jobIds;
}