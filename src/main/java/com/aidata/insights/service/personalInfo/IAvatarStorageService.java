package com.aidata.insights.service.personalInfo;

import org.springframework.web.multipart.MultipartFile;

/**
 * 头像存储服务
 *
 * <p>当前实现写入本地磁盘，后续切换对象存储（OSS / COS / MinIO）时
 * 只需替换实现，调用方无需改动。</p>
 */
public interface IAvatarStorageService {

    /**
     * 保存头像文件
     *
     * @param userId 当前登录用户 ID，用于隔离目录
     * @param file   头像文件
     * @return 相对 context-path 的访问地址，如 /uploads/avatar/36/202611/xxx.png
     */
    String store(Long userId, MultipartFile file);
}
