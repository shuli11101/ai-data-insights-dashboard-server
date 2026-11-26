package com.aidata.insights.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位分类及岗位数，供 AI 行情数据注入的「仅城市」兜底场景使用
 *
 * <p>触发时机：用户只给了城市、没给出可识别的岗位方向，此时不再按方向过滤，
 * 改为统计该城市下各二级分类（l2）分别有多少岗位。</p>
 */
@Data
@Schema(description = "岗位分类及岗位数")
public class AiCategoryCountVO {

    /**
     * 二级分类，如 后端开发、数据
     */
    @Schema(description = "二级分类", example = "后端开发")
    private String category;

    /**
     * 该分类在本城市的岗位数
     */
    @Schema(description = "岗位数", example = "107")
    private Long jobCount;
}