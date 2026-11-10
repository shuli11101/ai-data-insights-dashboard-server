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
 * 岗位信息（表 job_positions）
 *
 * <p>autoResultMap 必须开启，否则 skill_tags（json 列）无法反序列化为 List。</p>
 */
@Data
@TableName(value = "job_positions", autoResultMap = true)
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
     * 发布日期
     */
    private LocalDate publishDate;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
