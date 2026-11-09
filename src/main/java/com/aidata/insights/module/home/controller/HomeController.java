package com.aidata.insights.module.home.controller;

import com.aidata.insights.common.api.Result;
import com.aidata.insights.module.home.dto.JobQuery;
import com.aidata.insights.module.home.service.IJobPositionService;
import com.aidata.insights.module.home.vo.JobListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/home")
@RequiredArgsConstructor
public class HomeController {

    private final IJobPositionService jobPositionService;

    @GetMapping("/jobList")
    public Result<JobListVO> jobList(JobQuery query) {
        return Result.ok(jobPositionService.pageJobs(query));
    }
}
