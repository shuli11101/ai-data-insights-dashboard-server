package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 岗位匹配分析报告（表 job_match_report）
 */
@Data
@TableName("job_match_report")
public class MatchReport implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报告归属用户，对应 app_user.id
     */
    private Long userId;

    /**
     * 被分析的岗位，对应 job_positions.id
     */
    private Long jobId;

    /**
     * 综合匹配分
     */
    private Integer score;

    /**
     * Markdown 报告正文，技能覆盖率、薪资偏差等指标内嵌在正文中
     */
    private String content;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
