package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户求职意向（表 user_preference）
 *
 * <p>表没有自增 id，主键就是 user_id，一个用户一条意向。</p>
 */
@Data
@TableName(value = "user_preference", autoResultMap = true)
public class JobIntention implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键即用户 ID，对应 app_user.id，插入时需显式赋值
     */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private String city;

    /**
     * 期望岗位名称，对应列 job_title（存名称，不是 job_positions.id）
     */
    private String jobTitle;

    private String experience;

    private String education;

    /**
     * 技能标签，对应 json 列 skill_tags
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skillTags;

    private Integer currentSalary;

    /**
     * 期望薪资，对应列 expect_salary
     */
    private Integer expectSalary;

    private String jobStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
