package com.aidata.insights.service.personalInfo.impl;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.config.properties.UploadProperties;
import com.aidata.insights.service.personalInfo.IAvatarStorageService;
import com.aidata.insights.service.personalInfo.support.AvatarFileValidator;
import com.aidata.insights.service.personalInfo.support.AvatarKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 头像存储实现：写入本地磁盘目录，由静态资源映射对外提供访问
 *
 * <p>只适用于单实例部署；容器化或多实例部署请切换 app.upload.storage-type=s3。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.upload", name = "storage-type", havingValue = "local", matchIfMissing = true)
public class LocalAvatarStorageServiceImpl implements IAvatarStorageService {

    private final UploadProperties uploadProperties;

    private final AvatarFileValidator avatarFileValidator;

    /**
     * 存储头像文件
     *
     * @param userId 用户ID
     * @param file   头像文件
     * @return 头像文件 URL
     */
    @Override
    public String store(Long userId, MultipartFile file) {
        AvatarFileValidator.ValidatedAvatar avatar = avatarFileValidator.validate(file);

        // 文件名用 UUID，绝不使用原始文件名，避免路径穿越与重名覆盖
        String relativePath = AvatarKeys.next(userId, avatar.extension());

        Path root = Paths.get(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            // 防御性校验，正常流程不会命中
            throw new BizException(ResultCode.BAD_REQUEST, "头像文件名不合法");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.write(target, avatar.content());
        } catch (IOException e) {
            log.error("头像写入磁盘失败: {}", target, e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "头像保存失败，请稍后重试");
        }

        return uploadProperties.getAvatarUrlPrefix() + "/" + relativePath;
    }
}
