package com.aidata.insights.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 当前登录用户（从 JWT 中解析得到，无状态，不查库）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * app_user.id
     */
    private Long userId;

    /**
     * 手机号
     */
    private String phone;
}
