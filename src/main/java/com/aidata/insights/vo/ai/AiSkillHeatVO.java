package com.aidata.insights.vo.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 岗位技能热度，供 AI 行情数据注入使用
 *
 * <p>与首页的 SkillTagVO 字段相同但用途不同：这里的标签已按 AI 口径过滤掉
 * 经验、专业、教学内容、平台名等噪声，不直接对外暴露。</p>
 */
@Data
@Schema(description = "岗位技能热度")
public class AiSkillHeatVO {

    /**
     * 技能标签，已去空格并转小写
     */
    @Schema(description = "技能标签（小写）", example = "sql")
    private String skillTag;

    /**
     * 提及该技能的岗位数
     */
    @Schema(description = "提及该技能的岗位数", example = "36")
    private Long hotCount;
}