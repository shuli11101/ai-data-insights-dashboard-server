package com.aidata.insights.vo.personalInfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 个人信息（app_user + 求职意向 + 报告/收藏统计）
 */
@Data
@Schema(description = "个人信息")
public class PersonalInfoVO {

    @Schema(description = "用户 ID", example = "36")
    private Long id;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "昵称", example = "job_abc123")
    private String nickname;

    @Schema(description = "头像地址", example = "/api/uploads/xxx.png")
    private String avatar;

    @Schema(description = "目前职业", example = "高级前端工程师")
    private String currentJob;

    @Schema(description = "所在地", example = "深圳")
    private String location;

    @Schema(description = "工作经验，取自求职意向，未填写时为 null", example = "3-7年")
    private String experience;

    @Schema(description = "匹配报告数量", example = "3")
    private Integer reportCount;

    @Schema(description = "匹配报告平均分，无报告时为 0", example = "18")
    private Integer averageScore;

    @Schema(description = "收藏岗位数量", example = "2")
    private Integer favoriteCount;
}
