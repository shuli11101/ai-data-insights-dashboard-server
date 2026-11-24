package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位信息（表 job_positions_v2）
 *
 * <p>autoResultMap 必须开启，否则 skill_tags（json 列）无法反序列化为 List。</p>
 *
 * <p>v2 没有 created_at 列，采集时间对应 crawled_at；其余业务字段（job_id / l1~l3 / source 等）未接入。</p>
 */
@Data
@TableName(value = "job_positions_v2", autoResultMap = true)
public class JobPosition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String jobName;

    private String company;

    private String city;

    private String district;

    private Integer salaryMin;

    private Integer salaryMax;

    /**
     * 经验要求，文本形式：1年经验 / 3-4年经验 / 10年以上经验
     */
    private String experience;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skillTags;

    /**
     * 薪资展示文本（v2 专有）：如 5-10K / 面议 / 4900-5000元/时
     *
     * <p>为空的 salary_min/max 岗位在这一列给出「面议」或时薪描述，展示时优先用它。</p>
     */
    private String salaryDesc;

    /**
     * 学历要求：本科 / 大专 / 学历不限 等（v2 起单独成列，不再混在 skill_tags）
     */
    private String degree;

    /**
     * 发布日期
     */
    private LocalDate publishDate;

    /**
     * 采集时间（v2 对应列 crawled_at，v1 的 created_at 已不存在）
     */
    private LocalDateTime crawledAt;
}
