package com.aidata.insights.vo.personalInfo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 求职意向（表 user_preference 的对外视图）
 *
 * <p>主键是 user_id，属于当前登录用户，不对外输出；技能标签为 JSON 数组。</p>
 */
@Data
@Schema(description = "求职意向")
public class JobIntentionVO {

    @Schema(description = "城市", example = "成都")
    private String city;

    @Schema(description = "期望岗位名称", example = "Web前端开发工程师")
    private String jobTitle;

    @Schema(description = "工作经验", example = "3-5年")
    private String experience;

    @Schema(description = "教育背景", example = "本科")
    private String education;

    @Schema(description = "技能标签", example = "[\"Java\",\"Spring Boot\",\"MySQL\"]")
    private List<String> skills;

    @Schema(description = "当前薪资", example = "15000")
    private Integer currentSalary;

    @Schema(description = "期望薪资", example = "20000")
    private Integer expectedSalary;

    @Schema(description = "求职状态", example = "离职-随时到岗")
    private String jobStatus;

    @Schema(description = "更新时间", example = "2026-11-16 20:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
