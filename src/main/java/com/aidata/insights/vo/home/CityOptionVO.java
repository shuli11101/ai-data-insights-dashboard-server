package com.aidata.insights.vo.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 城市下拉选项
 *
 * <p>数据取自 city 表的 name 列，去掉「市」后缀以与 job_positions.city 的取值口径一致。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "城市下拉选项")
public class CityOptionVO {

    /**
     * 城市名，如 石家庄、三亚
     */
    @Schema(description = "城市名（不带「市」后缀）", example = "石家庄")
    private String city;
}
