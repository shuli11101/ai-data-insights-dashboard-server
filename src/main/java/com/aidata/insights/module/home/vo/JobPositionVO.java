package com.aidata.insights.module.home.vo;

import lombok.Data;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
import org.springframework.beans.BeanUtils;
import com.aidata.insights.module.home.entity.JobPosition;

@Data 
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)   // jobName → job_name
public class JobPositionVO {
    private Long id;
    private String jobName;
    private String company;
    private Integer salaryMin;
    private Integer salaryMax;
    private String experience;
    private List<String> skillTags;

    public static JobPositionVO from(JobPosition e) {
        JobPositionVO v = new JobPositionVO();
        BeanUtils.copyProperties(e, v);
        return v;
    }
}