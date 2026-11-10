package com.aidata.insights.common.constant;

/**
 * Redis key 常量
 *
 * <p>所有 key 前缀统一在此声明，动态 key 通过静态方法拼接，避免业务代码里散落字符串字面量。</p>
 *
 * <p>命名约定：{@code 业务域:用途:标识}，以冒号分隔，便于 redis-cli 按前缀扫描与批量清理。</p>
 */
public final class RedisKeyConstant {

    /**
     * Spring Cache 的 key 前缀，与 RedisCacheManager 的 computePrefixWith 保持一致
     */
    public static final String CACHE_PREFIX = "cache:";

    /**
     * 登录 / 注册验证码，标识为手机号
     */
    public static final String VERIFY_CODE_PREFIX = "auth:verify-code:";

    /**
     * 登录失败次数，标识为手机号，用于限流与账号锁定
     */
    public static final String LOGIN_FAIL_PREFIX = "auth:login-fail:";

    /**
     * 已注销令牌黑名单，标识为令牌本身，TTL 与令牌剩余有效期一致
     */
    public static final String TOKEN_BLACKLIST_PREFIX = "auth:token-blacklist:";

    /**
     * 岗位详情
     */
    public static final String JOB_DETAIL_PREFIX = "job:detail:";
    public static final int JOB_DETAIL_PREFIX_TTL = 60 * 60 * 24;

    /**
     * 首页统计结果：薪资区间 / 技术栈热度 / 城市分布 / 经验薪资
     */
    public static final String HOME_SALARY_RANGE = "home:salary-range";
    public static final String HOME_SKILL_TAGS = "home:skill-tags";
    public static final String HOME_DISTRICT = "home:district";
    public static final String HOME_EXPERIENCE_SALARY = "home:experience-salary";

    /**
     * 首页统计缓存有效期（秒），1 小时
     */
    public static final int HOME_STAT_TTL = 60 * 60;

    /**
     * 用户收藏岗位 ID 集合（Redis Set），标识为用户 ID
     */
    public static final String USER_FAVORITE_PREFIX = "user:favorite:";

    /**
     * 用户收藏集合有效期（秒），24 小时
     */
    public static final int USER_FAVORITE_TTL = 60 * 60 * 24;

    private RedisKeyConstant() {
    }

    public static String verifyCode(String phone) {
        return VERIFY_CODE_PREFIX + phone;
    }

    public static String loginFail(String phone) {
        return LOGIN_FAIL_PREFIX + phone;
    }

    public static String tokenBlacklist(String token) {
        return TOKEN_BLACKLIST_PREFIX + token;
    }

    public static String jobDetail(String id) {
        return JOB_DETAIL_PREFIX + id;
    }

    public static String userFavorites(Long userId) {
        return USER_FAVORITE_PREFIX + userId;
    }
}
