package com.aidata.insights.service.home.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.dto.home.JobQuery;
import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.mapper.home.CityMapper;
import com.aidata.insights.mapper.home.JobPositionMapper;
import com.aidata.insights.service.home.IJobPositionService;
import com.aidata.insights.service.user.FavoriteService;
import com.aidata.insights.vo.home.AreaJobCountVO;
import com.aidata.insights.vo.home.CityOptionVO;
import com.aidata.insights.vo.home.ExperienceSalaryVO;
import com.aidata.insights.vo.home.JobDetailVO;
import com.aidata.insights.vo.home.JobListVO;
import com.aidata.insights.vo.home.JobPositionVO;
import com.aidata.insights.vo.home.SalaryRangeVO;
import com.aidata.insights.vo.home.SkillTagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobPositionServiceImpl implements IJobPositionService {

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final JobPositionMapper jobPositionMapper;

    private final CityMapper cityMapper;

    private final FavoriteService favoriteService;

    
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

    // 根据id查询岗位详情
    @Override
    public JobDetailVO getJobDetail(String id) {
        String key = RedisKeyConstant.jobDetail(id);

        JobDetailVO cached = readCache(key);
        if (cached != null) {
            // 缓存里不含用户态，返回前按当前登录用户补上收藏状态
            cached.setFavorited(favoriteService.isFavorited(cached.getId()));
            return cached;
        }

        JobPosition jobPosition = jobPositionMapper.selectById(id);
        if (jobPosition == null) {
            throw new BizException(ResultCode.NOT_FOUND, "岗位不存在");
        }

        // 转为VO并写入缓存（缓存不含用户收藏状态），返回前再设置 favorited
        JobDetailVO detail = JobDetailVO.from(jobPosition);
        writeCache(key, detail);
        detail.setFavorited(favoriteService.isFavorited(jobPosition.getId()));
        return detail;
    }

    // 按薪资区间统计岗位数量(图)
    @Override
    public List<SalaryRangeVO> salaryRange() {
        return getOrLoad(RedisKeyConstant.HOME_SALARY_RANGE, SalaryRangeVO.class,
                jobPositionMapper::countBySalaryRange);
    }

    // 技术栈热度 Top 10
    @Override
    public List<SkillTagVO> hotSkillTags() {
        return getOrLoad(RedisKeyConstant.HOME_SKILL_TAGS, SkillTagVO.class,
                jobPositionMapper::selectHotSkillTags);
    }

    // 城市岗位数量（前 8 + 其他）
    @Override
    public List<AreaJobCountVO> districtJobCounts() {
        return getOrLoad(RedisKeyConstant.HOME_DISTRICT, AreaJobCountVO.class,
                jobPositionMapper::selectCityJobCounts);
    }

    // 按工作经验分档统计平均薪资
    @Override
    public List<ExperienceSalaryVO> experienceSalary() {
        return getOrLoad(RedisKeyConstant.HOME_EXPERIENCE_SALARY, ExperienceSalaryVO.class,
                jobPositionMapper::selectExperienceSalary);
    }

    // 城市下拉选项
    @Override
    public List<CityOptionVO> cityOptions() {
        return cityMapper.selectCityOptions();
    }

    // 读取缓存，JSON 无法解析时按未命中处理
    private JobDetailVO readCache(String key) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, JobDetailVO.class);
        } catch (JsonProcessingException e) {
            log.warn("岗位详情缓存反序列化失败, 按未命中处理: key={}", key);
            return null;
        }
    }

    // 写入缓存，一次性带过期时间
    private void writeCache(String key, JobDetailVO detail) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(detail),
                    Duration.ofSeconds(RedisKeyConstant.JOB_DETAIL_PREFIX_TTL));
        } catch (JsonProcessingException e) {
            log.warn("岗位详情序列化失败, 跳过缓存: key={}", key, e);
        }
    }

    // 统计类结果：查缓存 → 命中返回 → 未命中查库 → 回填；Redis 异常时降级直查库
    private <T> List<T> getOrLoad(String key, Class<T> elementType, Supplier<List<T>> loader) {
        JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, elementType);

        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(raw)) {
                return objectMapper.readValue(raw, type);
            }
        } catch (Exception e) {
            log.warn("首页统计缓存读取失败, 回源查库: key={}", key, e);
        }

        List<T> result = loader.get();
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result),
                    Duration.ofSeconds(RedisKeyConstant.HOME_STAT_TTL));
        } catch (Exception e) {
            log.warn("首页统计缓存写入失败, 跳过缓存: key={}", key, e);
        }
        return result;
    }
}
