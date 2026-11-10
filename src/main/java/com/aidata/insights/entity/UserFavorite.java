package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户收藏（表 user_favorite）
 *
 * <p>user_id + job_id 上有唯一索引 uk_user_job，重复收藏由数据库兜底。</p>
 */
@Data
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 收藏人，对应 app_user.id
     */
    private Long userId;

    /**
     * 被收藏岗位，对应 job_positions.id
     */
    private Long jobId;

    /**
     * 收藏时间，由数据库默认值 CURRENT_TIMESTAMP 填充
     */
    private LocalDateTime createTime;
}
