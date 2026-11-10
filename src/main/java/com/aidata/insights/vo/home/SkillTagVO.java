package com.aidata.insights.vo.home;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技术栈热度统计
 *
 * <p>字段按前端既有契约全部输出下划线：skill_tag、hot_count。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "技术栈热度")
public class SkillTagVO {

    /**
     * 技能标签，已统一转小写
     */
    @Schema(description = "技能标签（小写）", example = "excel")
    @JsonProperty("skill_tag")
    private String skillTag;

    /**
     * 提及该标签的岗位数量
     */
    @Schema(description = "提及该标签的岗位数量", example = "2751")
    @JsonProperty("hot_count")
    private Long hotCount;
}
