package com.aidata.insights.config;

import com.aidata.insights.config.properties.UploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Web 资源配置：把上传根目录映射为静态资源路径
 *
 * <p>仅在 app.upload.storage-type=local 时生效；对象存储模式下头像由存储服务直接返回 URL。</p>
 *
 * <p>注意：{@code server.servlet.context-path=/api}，容器会先剥掉 context-path，
 * 因此这里的 pattern 必须写 {@code /uploads/**}，对外访问地址才是 {@code /api/uploads/**}。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.upload", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class WebMvcConfig implements WebMvcConfigurer {

    /** 上传文件的对外映射路径，相对于 context-path */
    private static final String UPLOADS_URL_PATTERN = "/uploads/**";

    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadProperties.getRootDir()).toAbsolutePath().normalize();
        Path avatar = Paths.get(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        if (!avatar.startsWith(root)) {
            log.warn("app.upload.avatar-dir({}) 不在 app.upload.root-dir({}) 下，本地模式下头像将无法通过静态资源访问",
                    avatar, root);
        }

        registry.addResourceHandler(UPLOADS_URL_PATTERN)
                .addResourceLocations(toDirectoryLocation(root))
                .setCachePeriod(3600);
    }

    /**
     * Spring 要求目录形式的 location 以 / 结尾，否则相对路径解析会出错
     */
    private String toDirectoryLocation(Path dir) {
        String location = dir.toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
