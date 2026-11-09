package com.aidata.insights.security;

import com.aidata.insights.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 令牌生成与解析（HS256）
 *
 * <p>载荷沿用既有服务：sub = 用户 ID，phone = 手机号。
 * 使用同一 JWT_SECRET 时，Node 端历史签发的令牌同样可被本服务解析。</p>
 */
@Component
public class JwtTokenProvider {

    private static final int MIN_SECRET_LENGTH = 32;

    private static final String CLAIM_PHONE = "phone";

    private final JwtProperties properties;

    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = Optional.ofNullable(properties.getSecret())
                .orElse("")
                .getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("security.jwt.secret 长度不能少于 " + MIN_SECRET_LENGTH + " 个字符");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 签发令牌
     */
    public TokenInfo generateToken(Long userId, String phone) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getExpireMinutes(), ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_PHONE, phone)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        return new TokenInfo(token, expiresAt);
    }

    /**
     * 解析令牌，签名错误、格式错误或已过期都会抛出 {@link JwtException}
     */
    public JwtPayload parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtPayload(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_PHONE, String.class),
                claims.getExpiration().toInstant()
        );
    }

    /**
     * 令牌有效期（秒）
     */
    public long getExpireSeconds() {
        return properties.getExpireMinutes() * 60;
    }

    public record TokenInfo(String token, Instant expiresAt) {
    }

    public record JwtPayload(Long userId, String phone, Instant expiresAt) {
    }
}
