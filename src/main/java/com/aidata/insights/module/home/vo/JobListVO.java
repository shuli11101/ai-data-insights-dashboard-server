package com.aidata.insights.module.home.vo;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.util.List;

@Data 
@AllArgsConstructor
public class JobListVO {
    private List<JobPositionVO> list;
    private long total;
}
