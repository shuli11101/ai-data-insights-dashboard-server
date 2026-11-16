package com.aidata.insights.mapper.personalInfo;

import com.aidata.insights.entity.JobIntention;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户求职意向 Mapper
 */
@Mapper
public interface IJobIntentionMapper extends BaseMapper<JobIntention> {
}