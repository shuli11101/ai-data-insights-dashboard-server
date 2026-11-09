package com.aidata.insights.module.home.dto;

import lombok.Data;

@Data 
public class JobQuery {
    private long page = 1;
    private long size = 10;
    private String city;
    private Integer experienceMin;
    private Integer experienceMax;
    private Integer salaryMin;
    private Integer salaryMax;
    private String keyword;
    private String jobName;
}
