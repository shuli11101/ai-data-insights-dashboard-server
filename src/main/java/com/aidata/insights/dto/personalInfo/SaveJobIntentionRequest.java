package com.aidata.insights.dto.personalInfo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 保存 / 更新求职意向请求
 *
 * <p>技能字段为 skillTags（落库 skill_tags），期望薪资为 expectedSalary（落库 expect_salary）。</p>
 */
@Data
@Schema(description = "保存求职意向请求")
public class SaveJobIntentionRequest {

    @Schema(description = "意向城市", example = "成都", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择意向城市")
    @Size(max = 32, message = "意向城市不能超过 32 个字")
    private String city;

    @Schema(description = "工作经验", example = "3-5年", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "请选择工作经验")
    @Size(max = 32, message = "工作经验不能超过 32 个字")
    private String experience;

    @Schema(description = "技能标签", example = "[\"Vue\",\"React\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请至少填写一项技能")
    @Size(max = 30, message = "技能最多 30 项")
    @JsonAlias("skills")
    private List<@NotBlank(message = "技能不能为空")
                 @Size(max = 20, message = "单项技能不能超过 20 个字") String> skillTags;

    @Schema(description = "意向岗位", example = "Web前端开发工程师")
    @Size(max = 64, message = "意向岗位不能超过 64 个字")
    private String jobTitle;

    @Schema(description = "学历", example = "本科")
    @Size(max = 32, message = "学历不能超过 32 个字")
    private String education;

    @Schema(description = "求职状态", example = "离职，随时到岗")
    @Size(max = 16, message = "求职状态不能超过 16 个字")
    private String jobStatus;

    @Schema(description = "当前薪资", example = "15")
    @PositiveOrZero(message = "当前薪资不能为负数")
    private Integer currentSalary;

    @Schema(description = "期望薪资", example = "20")
    @PositiveOrZero(message = "期望薪资不能为负数")
    private Integer expectedSalary;
}