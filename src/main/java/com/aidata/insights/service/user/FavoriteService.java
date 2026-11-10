package com.aidata.insights.service.user;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.entity.JobPosition;
import com.aidata.insights.entity.UserFavorite;
import com.aidata.insights.mapper.home.JobPositionMapper;
import com.aidata.insights.mapper.user.UserFavoriteMapper;
import com.aidata.insights.security.SecurityUtils;
import com.aidata.insights.vo.user.FavoriteJobVO;
import com.aidata.insights.vo.user.FavoriteListVO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户收藏服务
 *
 * <p>收藏记录以 user_favorite 为准；Redis 缓存「用户收藏岗位 ID 集合」，
 * 写操作只失效缓存（cache-aside），读取未命中时再回源重建。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final UserFavoriteMapper userFavoriteMapper;

    private final JobPositionMapper jobPositionMapper;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 收藏岗位：岗位不存在报 404，已收藏时按幂等处理
     */
    public void addFavorite(Long jobId) {
        Long userId = SecurityUtils.requireUserId();

        if (jobPositionMapper.selectById(jobId) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "岗位不存在");
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(userId);
        favorite.setJobId(jobId);
        try {
            userFavoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 唯一索引 uk_user_job 兜底：已经收藏过，直接返回成功
        }
        // 写库成功后只失效缓存，等读取时再回源重建
        evictCache(userId);
    }

    /**
     * 取消收藏：未收藏时按幂等处理
     */
    public void removeFavorite(Long jobId) {
        Long userId = SecurityUtils.requireUserId();
        userFavoriteMapper.delete(Wrappers.<UserFavorite>lambdaQuery()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getJobId, jobId));
        // 写库成功后只失效缓存，等读取时再回源重建
        evictCache(userId);
    }

    /**
     * 当前登录用户的收藏列表（按收藏时间倒序分页），并用全量 jobId 重建收藏集合缓存
     */
    public FavoriteListVO listFavorites(long page, long size) {
        Long userId = SecurityUtils.requireUserId();

        Page<UserFavorite> favoritePage = userFavoriteMapper.selectPage(new Page<>(page, size),
                Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime)
                        .orderByDesc(UserFavorite::getId));

        List<UserFavorite> records = favoritePage.getRecords();
        List<FavoriteJobVO> favorites = List.of();
        if (!records.isEmpty()) {
            List<Long> jobIds = records.stream().map(UserFavorite::getJobId).toList();
            // 走内置批量查询以复用实体的 JSON typeHandler（skill_tags）
            Map<Long, JobPosition> byId = jobPositionMapper.selectBatchIds(jobIds).stream()
                    .collect(Collectors.toMap(JobPosition::getId, p -> p));
            favorites = records.stream()
                    .filter(favorite -> byId.containsKey(favorite.getJobId()))
                    .map(favorite -> FavoriteJobVO.from(
                            byId.get(favorite.getJobId()), favorite.getCreateTime()))
                    .toList();
        }

        // 列表查询是读取入口：顺便用全量 jobId 重建收藏集合，供详情页 favorited 判断复用
        rebuildCache(RedisKeyConstant.userFavorites(userId), loadFavoriteIds(userId));
        return new FavoriteListVO(favorites, favoritePage.getTotal());
    }

    /**
     * 当前登录用户是否收藏了该岗位；未登录直接返回 false
     */
    public boolean isFavorited(Long jobId) {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return false;
        }

        String key = RedisKeyConstant.userFavorites(userId);
        String jobIdText = String.valueOf(jobId);
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(key, jobIdText))) {
                return true;
            }
            // key 存在说明集合已完整加载，未命中即确实未收藏
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                return false;
            }
        } catch (Exception e) {
            log.warn("收藏集合缓存读取失败, 回源查库: key={}", key, e);
        }

        Set<String> favoriteIds = loadFavoriteIds(userId);
        rebuildCache(key, favoriteIds);
        return favoriteIds.contains(jobIdText);
    }

    private Set<String> loadFavoriteIds(Long userId) {
        List<Object> jobIds = userFavoriteMapper.selectObjs(Wrappers.<UserFavorite>lambdaQuery()
                .select(UserFavorite::getJobId)
                .eq(UserFavorite::getUserId, userId));
        return jobIds.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    private void rebuildCache(String key, Set<String> favoriteIds) {
        try {
            // 先清空再写入，保证取消收藏时旧元素不会残留
            stringRedisTemplate.delete(key);
            if (favoriteIds.isEmpty()) {
                return;
            }
            stringRedisTemplate.opsForSet().add(key, favoriteIds.toArray(String[]::new));
            stringRedisTemplate.expire(key, Duration.ofSeconds(RedisKeyConstant.USER_FAVORITE_TTL));
        } catch (Exception e) {
            log.warn("收藏集合缓存写入失败, 跳过缓存: key={}", key, e);
        }
    }

    // 写操作仅失效缓存：不为没人读的数据付出全量重建成本
    private void evictCache(Long userId) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.userFavorites(userId));
        } catch (Exception e) {
            log.warn("收藏集合缓存清理失败: userId={}", userId, e);
        }
    }
}
