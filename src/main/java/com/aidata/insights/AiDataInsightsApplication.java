package com.aidata.insights;

import com.aidata.insights.security.properties.CorsProperties;
import com.aidata.insights.security.properties.JwtProperties;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration;
import com.github.xiaoymin.knife4j.spring.configuration.insight.Knife4jInsightAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI 数据洞察看板后端服务启动类
 *
 * <p>排除项说明：</p>
 * <ul>
 *   <li>{@link UserDetailsServiceAutoConfiguration}：认证完全基于 JWT 过滤器，不需要默认内存用户</li>
 *   <li>{@link Knife4jAutoConfiguration} / {@link Knife4jInsightAutoConfiguration}：
 *       Knife4j 4.5.0 按 springdoc 2.2.0 编译，调用 {@code SpringDocConfigProperties.getGroupConfigs()}
 *       期望返回 {@code List}，而 springdoc 2.8.x 已改为 {@code Set}，会在生成 /v3/api-docs 时抛
 *       {@code NoSuchMethodError} 导致 500。这两个自动配置只负责增强排序/自定义，禁用后
 *       {@code doc.html}（静态资源）与 springdoc 生成的接口文档仍然可用。</li>
 * </ul>
 *
 * @author aidata
 */
@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class,
        Knife4jAutoConfiguration.class,
        Knife4jInsightAutoConfiguration.class
})
@MapperScan("com.aidata.insights.module.**.mapper")
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class AiDataInsightsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiDataInsightsApplication.class, args);
    }
}
