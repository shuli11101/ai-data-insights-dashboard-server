package com.aidata.insights.vo.home;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import com.aidata.insights.entity.JobPosition;

/**
 * 岗位详情
 *
 * <p>与 {@link JobPositionVO} 相比包含 city / district 等详情字段。</p>
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)   // jobName → job_name
@Schema(description = "岗位详情")
public class JobDetailVO {

    @Schema(description = "岗位 ID", example = "24014")
    private Long id;

    @Schema(description = "岗位名称", example = "数据分析师")
    private String jobName;

    @Schema(description = "公司名称", example = "某某科技")
    private String company;

    @Schema(description = "城市", example = "广州")
    private String city;

    @Schema(description = "区县", example = "天河区")
    private String district;

    @Schema(description = "最低薪资（K）", example = "10")
    private Integer salaryMin;

    @Schema(description = "最高薪资（K）", example = "15")
    private Integer salaryMax;

    @Schema(description = "薪资展示文本，优先用于展示（面议/时薪也在此列）", example = "5-10K")
    private String salaryDesc;

    @Schema(description = "经验要求", example = "1-3年")
    private String experience;

    @Schema(description = "学历要求", example = "本科")
    private String degree;

    @Schema(description = "技能标签")
    private List<String> skillTags;

    @Schema(description = "发布日期，v2 数据为空时不下发", example = "2025-07-07")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    @Schema(description = "采集时间（对应 v2 的 crawled_at）", example = "2025-07-07 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime crawledAt;

    /**
     * 当前登录用户是否已收藏，未登录时为 false
     */
    @Schema(description = "当前用户是否已收藏，未登录为 false", example = "false")
    private boolean favorited = false;

    public static JobDetailVO from(JobPosition e) {
        JobDetailVO v = new JobDetailVO();
        BeanUtils.copyProperties(e, v);
        return v;
    }


}
