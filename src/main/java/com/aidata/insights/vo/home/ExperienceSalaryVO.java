package com.aidata.insights.vo.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作经验与平均薪资统计
 *
 * <p>字段按前端既有契约：experience 保持不变，平均薪资输出下划线 avg_salary。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "工作经验与平均薪资")
public class ExperienceSalaryVO {

    /**
     * 经验档位：无需经验 / 1-3年 / 3-5年 / ≥5年
     */
    @Schema(description = "经验档位", example = "1-3年")
    private String experience;

    /**
     * 该档位平均薪资（K，四舍五入取整）
     */
    @Schema(description = "平均薪资（K）", example = "8")
    @JsonProperty("avg_salary")
    private Integer avgSalary;
}
