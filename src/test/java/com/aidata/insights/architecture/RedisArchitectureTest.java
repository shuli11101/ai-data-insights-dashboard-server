package com.aidata.insights.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Redis 使用约束
 *
 * <p>Spring Boot 的 RedisAutoConfiguration 会无条件提供 {@code RedisTemplate<Object, Object>}（JDK 序列化），
 * 即使项目里没有自定义 RedisTemplate，业务代码 {@code @Autowired RedisTemplate<String, Object>} 依然能注入成功，
 * 进而在 Redis 中写入 {@code \xAC\xED} 开头的二进制数据，与 {@code RedisService} 写入的 JSON 互不兼容。</p>
 *
 * <p>因此用架构测试把这条约束自动化：除 {@code RedisService} 外，任何类都不得依赖
 * {@code org.springframework.data.redis.core.RedisTemplate}。违反时 {@code mvn test} 直接失败。</p>
 */
class RedisArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.aidata.insights");

    @Test
    @DisplayName("除 RedisService 外，禁止直接依赖 RedisTemplate")
    void onlyRedisServiceMayDependOnRedisTemplate() {
        assertDoesNotThrow(() -> noClasses()
                .that().doNotHaveSimpleName("RedisService")
                .should().dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.data.redis.core.RedisTemplate")
                .because("业务代码应使用 RedisService（基于 StringRedisTemplate + JSON），"
                        + "直接注入 RedisTemplate 会拿到自动配置的 JDK 序列化版本，写入二进制乱码")
                .check(PRODUCTION_CLASSES));
    }
}
