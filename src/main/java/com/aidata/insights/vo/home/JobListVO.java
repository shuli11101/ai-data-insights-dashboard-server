package com.aidata.insights.vo.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 岗位列表结果
 */
@Data
@AllArgsConstructor
@Schema(description = "岗位列表结果")
public class JobListVO {

    @Schema(description = "当前页岗位")
    private List<JobPositionVO> list;

    @Schema(description = "总条数", example = "24014")
    private long total;
}
