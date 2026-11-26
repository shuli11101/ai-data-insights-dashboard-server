package com.aidata.insights.service.ai.impl;

import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 行情注入服务的本地验证：依赖本地 MySQL 与 Redis
 *
 * <p>命名用 IT 后缀，不进入 Surefire 默认执行范围，避免 CI 环境没有数据库时失败。
 * 手动运行：{@code mvn test -Dtest=AiMarketContextServiceIT}</p>
 */
@SpringBootTest
class AiMarketContextServiceIT {

    @Autowired
    private AiMarketContextService aiMarketContextService;

    @Autowired
    private RedisService redisService;

    /**
     * 清掉样本涉及的快照，否则第二轮会命中缓存，验证不到最新的 SQL 过滤规则
     */
    @BeforeEach
    void cleanSnapshotCache() {
        redisService.delete(RedisKeyConstant.aiMarket("深圳", null));
        redisService.delete(RedisKeyConstant.aiMarket("深圳", "数据"));
        redisService.delete(RedisKeyConstant.aiMarket("深圳", "后端开发"));
        redisService.delete(RedisKeyConstant.aiMarket("深圳", "前端/移动开发"));
        redisService.delete(RedisKeyConstant.aiMarket("广州", "运营"));
    }

    @Test
    void printInjectionText() {
        List<String> samples = List.of(
                // 组合规则：python+sql → 数据
                "我会 Python 和 SQL，深圳，3年经验，期望 25k",
                // 组合规则：python+django → 后端开发
                "我会 Python 和 Django，深圳，2年经验",
                // 单技能：python → 后端开发
                "我会 Python，深圳",
                // 单技能冲突：java(后端) vs sql(数据) → 不反推，落 CITY_ONLY
                "我会 Java 和 SQL，深圳",
                // 词典优先："数据分析"命中词典，不走反推
                "我做数据分析，会 Python 和 SQL，深圳，3年经验",
                // 城市+方向都有数据 → CITY_DIRECTION
                "我在广州做运营，帮我看看值多少钱"
        );
        for (String sample : samples) {
            System.out.println("===== 输入: " + sample);
            System.out.println(aiMarketContextService.build(sample));
            System.out.println();
        }
    }
}