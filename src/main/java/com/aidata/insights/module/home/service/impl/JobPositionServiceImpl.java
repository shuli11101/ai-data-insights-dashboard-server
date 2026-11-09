package com.aidata.insights.module.home.service.impl;

import com.aidata.insights.module.home.dto.JobQuery;
import com.aidata.insights.module.home.entity.JobPosition;
import com.aidata.insights.module.home.mapper.JobPositionMapper;
import com.aidata.insights.module.home.service.IJobPositionService;
import com.aidata.insights.module.home.vo.JobListVO;
import com.aidata.insights.module.home.vo.JobPositionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JobPositionServiceImpl implements IJobPositionService {

    private final JobPositionMapper jobPositionMapper;

    
    // 查询岗位列表，包含多个可能为空条件
    @Override
    public JobListVO pageJobs(JobQuery query) {
        LambdaQueryWrapper<JobPosition> wrapper = Wrappers.<JobPosition>lambdaQuery()
                .like(StringUtils.hasText(query.getJobName()), JobPosition::getJobName, query.getJobName())
                .like(StringUtils.hasText(query.getKeyword()), JobPosition::getJobName, query.getKeyword())
                .eq(StringUtils.hasText(query.getCity()), JobPosition::getCity, query.getCity())
                .le(query.getSalaryMax() != null, JobPosition::getSalaryMin, query.getSalaryMax())
                .ge(query.getSalaryMin() != null, JobPosition::getSalaryMax, query.getSalaryMin())
                .orderByDesc(JobPosition::getId);

        Page<JobPosition> page = jobPositionMapper.selectPage(
                new Page<>(query.getPage(), query.getSize()), wrapper);

        return new JobListVO(
                page.getRecords().stream().map(JobPositionVO::from).toList(),
                page.getTotal());
    }
}
