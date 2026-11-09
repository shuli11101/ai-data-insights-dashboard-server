package com.aidata.insights.module.home.mapper;

import com.aidata.insights.module.home.entity.JobPosition;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 岗位 Mapper
 *
 * <p>查询条件全部在 Service 里用 LambdaQueryWrapper 组装，无需 XML 与自定义方法。</p>
 */
@Mapper
public interface JobPositionMapper extends BaseMapper<JobPosition> {
}
