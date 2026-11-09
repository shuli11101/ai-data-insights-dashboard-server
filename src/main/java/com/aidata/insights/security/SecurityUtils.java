package com.aidata.insights.security;

import com.aidata.insights.common.api.ResultCode;
import com.aidata.insights.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 当前登录用户工具类
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录返回空
     */
    public static Optional<CurrentUser> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            return Optional.empty();
        }
        return Optional.of(currentUser);
    }

    /**
     * 获取当前登录用户 ID，未登录时抛出 401
     */
    public static Long requireUserId() {
        return getCurrentUser()
                .map(CurrentUser::getUserId)
                .orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED));
    }

    /**
     * 获取当前登录用户，未登录时抛出 401
     */
    public static CurrentUser requireCurrentUser() {
        return getCurrentUser().orElseThrow(() -> new BizException(ResultCode.UNAUTHORIZED));
    }

    /**
     * 获取当前登录用户 ID，未登录返回 null（用于可选登录场景）
     */
    public static Long getUserIdOrNull() {
        return getCurrentUser().map(CurrentUser::getUserId).orElse(null);
    }
}
