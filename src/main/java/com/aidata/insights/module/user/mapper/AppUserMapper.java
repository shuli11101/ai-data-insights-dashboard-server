package com.aidata.insights.module.user.mapper;

import com.aidata.insights.module.user.entity.AppUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用用户 Mapper
 */
@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
}
