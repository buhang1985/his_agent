package com.hisagent.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;

/**
 * JWT Token 提供者
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:YUzh@1985223HisAgentSecretKeyForJWTTokenGeneration2026}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * 从 token 中提取用户名
     */
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 token 中提取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 提取指定 claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 生成 JWT token
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    /**
     * 创建 token
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * 验证 token 是否有效
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 检查 token 是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    /**
     * 提取所有 claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
