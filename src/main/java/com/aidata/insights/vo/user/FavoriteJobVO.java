package com.aidata.insights.vo.user;

import com.aidata.insights.entity.JobPosition;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收藏列表项
 *
 * <p>字段按前端契约输出驼峰；favoriteTime 来自 user_favorite.create_time，而非岗位表。</p>
 */
@Data
@Schema(description = "收藏列表项")
public class FavoriteJobVO {

    @Schema(description = "岗位 ID", example = "24014")
    private Long id;

    @Schema(description = "岗位名称", example = "在线班主任")
    private String jobName;

    @Schema(description = "公司名称", example = "成都书声科技有限公司")
    private String company;

    @Schema(description = "城市", example = "成都")
    private String city;

    @Schema(description = "区县", example = "高新区")
    private String district;

    @Schema(description = "最低薪资（K）", example = "4")
    private Integer salaryMin;

    @Schema(description = "最高薪资（K）", example = "7")
    private Integer salaryMax;

    @Schema(description = "经验要求", example = "1年经验")
    private String experience;

    @Schema(description = "技能标签")
    private List<String> skillTags;

    @Schema(description = "发布日期", example = "2025-07-07")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate;

    @Schema(description = "收藏时间", example = "2026-11-10 21:50:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime favoriteTime;

    public static FavoriteJobVO from(JobPosition job, LocalDateTime favoriteTime) {
        FavoriteJobVO vo = new FavoriteJobVO();
        BeanUtils.copyProperties(job, vo);
        vo.setFavoriteTime(favoriteTime);
        return vo;
    }
}
