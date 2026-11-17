package com.aidata.insights.vo.personalInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 头像上传结果
 */
@Data
@Schema(description = "头像上传结果")
public class AvatarVO {

    @Schema(description = "头像地址（已入库的值，相对 context-path）", example = "/uploads/avatar/36/202611/7f3c9a1e.png")
    private String avatar;

    @Schema(description = "可直接访问的完整地址", example = "/api/uploads/avatar/36/202611/7f3c9a1e.png")
    private String url;
}
