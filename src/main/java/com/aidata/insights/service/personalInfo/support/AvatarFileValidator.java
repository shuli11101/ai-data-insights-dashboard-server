package com.aidata.insights.service.personalInfo.support;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import com.aidata.insights.config.properties.UploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

/**
 * 头像文件校验：大小、扩展名与真实文件类型
 *
 * <p>本地磁盘与对象存储两种实现共用，避免校验规则不一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AvatarFileValidator {

    /** 识别图片类型所需的文件头字节数 */
    private static final int HEADER_LENGTH = 12;

    private final UploadProperties uploadProperties;

    /**
     * 校验并读出文件内容，扩展名与内容不一致时直接拒绝
     */
    public ValidatedAvatar validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "请选择要上传的头像文件");
        }
        if (file.getSize() > uploadProperties.getAvatarMaxSize()) {
            long limitMb = Math.max(1, uploadProperties.getAvatarMaxSize() / 1024 / 1024);
            throw new BizException(ResultCode.BAD_REQUEST, "头像大小不能超过 " + limitMb + "MB");
        }

        // jpg 与 jpeg 是同一种格式，统一按 jpg 处理
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        String normalized = "jpeg".equals(extension) ? "jpg" : extension;
        if (!uploadProperties.getAllowedExtensions().contains(normalized)) {
            throw new BizException(ResultCode.BAD_REQUEST, "头像仅支持 jpg、png、webp 格式");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            log.error("读取头像文件失败", e);
            throw new BizException(ResultCode.INTERNAL_ERROR, "头像读取失败，请稍后重试");
        }

        String detected = detectImageType(content);
        if (!normalized.equals(detected)) {
            // 扩展名与内容不一致（例如把脚本改名为 .png），直接拒绝
            throw new BizException(ResultCode.BAD_REQUEST, "头像文件内容与格式不一致");
        }
        return new ValidatedAvatar(detected, content);
    }

    /**
     * 通过文件头识别真实图片类型，无法识别时返回 null
     */
    private String detectImageType(byte[] content) {
        int read = Math.min(content.length, HEADER_LENGTH);
        // JPEG: FF D8 FF
        if (read >= 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        // PNG: 89 50 4E 47
        if (read >= 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P'
                && content[2] == 'N' && content[3] == 'G') {
            return "png";
        }
        // WebP: RIFF....WEBP
        if (read >= HEADER_LENGTH && content[0] == 'R' && content[1] == 'I' && content[2] == 'F'
                && content[3] == 'F' && content[8] == 'W' && content[9] == 'E'
                && content[10] == 'B' && content[11] == 'P') {
            return "webp";
        }
        return null;
    }

    /**
     * 校验通过的头像文件
     *
     * @param extension 归一化后的扩展名：jpg / png / webp
     * @param content   文件内容
     */
    public record ValidatedAvatar(String extension, byte[] content) {
    }
}
