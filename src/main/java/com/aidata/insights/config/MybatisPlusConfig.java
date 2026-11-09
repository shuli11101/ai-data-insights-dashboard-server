package com.aidata.insights.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 插件需按顺序添加：多租户 -> 动态表名 -> 分页 -> 乐观锁 -> 防全表更新删除
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        // 单页最大条数，防止恶意分页拖垮数据库
        pagination.setMaxLimit(500L);
        // 页码超过总页数时返回空列表而不是首页
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);

        // 乐观锁：实体中 @Version 字段生效
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防止误写全表 update / delete
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        return interceptor;
    }
}
