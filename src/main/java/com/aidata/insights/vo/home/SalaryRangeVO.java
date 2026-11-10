package com.aidata.insights.vo.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 薪资区间岗位统计
 *
 * <p>字段命名按前端既有契约：区间为下划线 salary_range，数量为驼峰 jobCount，
 * 因此不使用全局 SnakeCase 策略，改为逐字段显式声明。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "薪资区间岗位数量")
public class SalaryRangeVO {

    /**
     * 薪资区间，如 0-10K、40K+
     */
    @Schema(description = "薪资区间", example = "0-10K")
    @JsonProperty("salary_range")
    private String salaryRange;

    /**
     * 该区间岗位数量
     */
    @Schema(description = "岗位数量", example = "15518")
    @JsonProperty("jobCount")
    private Long jobCount;
}
