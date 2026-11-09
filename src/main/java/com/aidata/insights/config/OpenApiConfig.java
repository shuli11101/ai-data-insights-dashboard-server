package com.aidata.insights.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 接口文档配置（Knife4j + springdoc OpenAPI 3）
 *
 * <p>文档地址：http://localhost:8080/api/doc.html</p>
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer";

        Server localServer = new Server()
                .url("http://localhost:" + serverPort + contextPath)
                .description("本地开发环境");

        return new OpenAPI()
                .info(new Info()
                        .title("AI 数据洞察看板 API")
                        .description("AI 数据洞察看板后端服务接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("aidata").email("dev@aidata.com"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(localServer))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
