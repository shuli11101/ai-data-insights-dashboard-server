package com.aidata.insights.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用用户（对应既有表 app_user）
 *
 * <p>主键为数据库自增，登录标识为手机号，密码字段存储 scrypt$盐$哈希。</p>
 */
@Data
@TableName("app_user")
public class AppUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键，数据库自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 手机号，登录账号，唯一
     */
    private String phone;

    /**
     * scrypt 密码密文，格式：scrypt$盐$哈希
     */
    @JsonIgnore
    private String passwordHash;

    private String nickname;

    private String avatar;

    /**
     * 目前职业
     */
    private String currentJob;

    /**
     * 所在地
     */
    private String location;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
