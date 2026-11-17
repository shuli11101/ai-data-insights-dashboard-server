package com.aidata.insights.security;

import com.aidata.insights.common.constant.RedisKeyConstant;
import com.aidata.insights.common.redis.RedisService;
import com.aidata.insights.security.properties.JwtProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 Authorization: Bearer &lt;token&gt;，无状态恢复登录身份
 *
 * <p>与既有 Node 服务的 requireAuth 行为一致：只验签与有效期，不查库、不依赖 Redis。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_USER = "ROLE_USER";

    private final JwtTokenProvider jwtTokenProvider;

    private final JwtProperties jwtProperties;

    private final RedisService redisService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = jwtTokenProvider.resolveToken(request.getHeader(jwtProperties.getHeader()))
                .orElse(null);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null
                && !isBlacklisted(token)) {
            try {
                JwtTokenProvider.JwtPayload payload = jwtTokenProvider.parseToken(token);
                CurrentUser currentUser = new CurrentUser(payload.userId(), payload.phone());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(currentUser, null,
                                List.of(new SimpleGrantedAuthority(ROLE_USER)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                log.debug("令牌已过期: {}", e.getMessage());
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("令牌解析失败: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 黑名单校验：已注销的令牌即使签名有效也不再认。
     *
     * <p>Redis 不可用时降级放行，避免缓存故障导致全站鉴权失效。</p>
     */
    private boolean isBlacklisted(String token) {
        try {
            String key = RedisKeyConstant.tokenBlacklist(jwtTokenProvider.fingerprint(token));
            return redisService.hasKey(key);
        } catch (RuntimeException e) {
            log.warn("令牌黑名单校验失败，按未拉黑处理: {}", e.getMessage());
            return false;
        }
    }
}
