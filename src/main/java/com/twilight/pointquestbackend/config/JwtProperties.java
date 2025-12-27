package com.twilight.pointquestbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    /**
     * Secret used to sign JWT.
     */
    private String secret;
    /**
     * Expiration in minutes.
     */
    private long expiresMinutes = 120;

}
