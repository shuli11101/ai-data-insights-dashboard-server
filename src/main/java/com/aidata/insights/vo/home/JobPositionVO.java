package com.aidata.insights.vo.home;

import lombok.Data;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.beans.BeanUtils;
import com.aidata.insights.entity.JobPosition;

/**
 * 岗位列表项
 */
@Data 
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)   // jobName → job_name
@Schema(description = "岗位列表项")
public class JobPositionVO {

    @Schema(description = "岗位 ID", example = "24014")
    private Long id;

    @Schema(description = "岗位名称", example = "数据分析师")
    private String jobName;

    @Schema(description = "公司名称", example = "某某科技")
    private String company;

    @Schema(description = "最低薪资（K）", example = "10")
    private Integer salaryMin;

    @Schema(description = "最高薪资（K）", example = "15")
    private Integer salaryMax;

    @Schema(description = "经验要求", example = "1-3年经验")
    private String experience;

    @Schema(description = "技能标签")
    private List<String> skillTags;

    public static JobPositionVO from(JobPosition e) {
        JobPositionVO v = new JobPositionVO();
        BeanUtils.copyProperties(e, v);
        return v;
    }
}
