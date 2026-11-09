package com.aidata.insights.module.home.service;

import com.aidata.insights.module.home.dto.JobQuery;
import com.aidata.insights.module.home.vo.JobListVO;

public interface IJobPositionService {

    /**
     * 分页查询岗位列表
     */
    JobListVO pageJobs(JobQuery query);
}
