package com.twilight.pointquestbackend.security;

import com.twilight.pointquestbackend.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtService {
    private final JwtProperties properties;
    private SecretKey cachedKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(UserPrincipal principal) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiresAt = new Date(now + properties.getExpiresMinutes() * 60_000);
        return Jwts.builder()
                .setSubject(String.valueOf(principal.getId()))
                .claim("username", principal.getUsername())
                .claim("email", principal.getEmail())
                .claim("role", principal.getRole())
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Optional<UserPrincipal> parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            String email = claims.get("email", String.class);
            String role = claims.get("role", String.class);
            return Optional.of(new UserPrincipal(userId, username, email, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private SecretKey getKey() {
        if (cachedKey == null) {
            String secret = properties.getSecret();
            if (secret == null || secret.length() < 32) {
                throw new IllegalStateException("JWT secret is not configured properly");
            }
            cachedKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        return cachedKey;
    }
}
