package com.aidata.insights.service.personalInfo.support;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 头像对象 Key 生成：按用户与月份分目录，文件名用 UUID 避免重名覆盖
 */
public final class AvatarKeys {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private AvatarKeys() {
    }

    /**
     * 生成相对 Key，如 36/202611/7f3c9a1e5b8d2f6a.png
     */
    public static String next(Long userId, String extension) {
        return userId + "/"
                + LocalDate.now().format(MONTH_FORMATTER) + "/"
                + UUID.randomUUID().toString().replace("-", "") + "." + extension;
    }
}
