package com.aidata.insights.vo.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 城市岗位数量统计
 *
 * <p>字段按前端既有契约输出下划线：area_name、job_count。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "城市岗位数量")
public class AreaJobCountVO {

    /**
     * 城市名，尾部汇总项为「其他」
     */
    @Schema(description = "城市名，尾部汇总项为其他", example = "广州")
    @JsonProperty("area_name")
    private String areaName;

    /**
     * 该城市岗位数量
     */
    @Schema(description = "岗位数量", example = "3927")
    @JsonProperty("job_count")
    private Long jobCount;
}
