package com.twilight.pointquestbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.forgot-password")
public class ForgotPasswordProperties {
    /**
     * Reset URL template containing {token}.
     */
    private String resetUrlTemplate;
    /**
     * Token TTL in minutes.
     */
    private long tokenTtlMinutes = 30;
    /**
     * Email subject for password reset.
     */
    private String emailSubject = "Password reset";
    /**
     * Optional from address override.
     */
    private String emailFrom;
}
