package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.config.ForgotPasswordProperties;
import com.twilight.pointquestbackend.domain.Users;
import com.twilight.pointquestbackend.mapper.UsersMapper;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.Random;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {

    private static final String RESET_KEY_PREFIX = "auth:password-reset:";
    private static final int TOKEN_BYTES = 32;

    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final ForgotPasswordProperties forgotPasswordProperties;
    private final Random secureRandom = new java.security.SecureRandom();

    public PasswordResetService(UsersMapper usersMapper,
                                PasswordEncoder passwordEncoder,
                                StringRedisTemplate redisTemplate,
                                JavaMailSender mailSender,
                                ForgotPasswordProperties forgotPasswordProperties) {
        this.usersMapper = usersMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.forgotPasswordProperties = forgotPasswordProperties;
    }

    public void requestReset(String email) {
        if (!StringUtils.hasText(email)) {
            throw new ServiceException(400, "email_required");
        }
        Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
        if (user == null) {
            return;
        }
        String token = generateToken();
        String key = resetKey(token);
        Duration ttl = Duration.ofMinutes(Math.max(1, forgotPasswordProperties.getTokenTtlMinutes()));
        redisTemplate.opsForValue().set(key, user.getId().toString(), ttl);
        sendResetEmail(user.getEmail(), token);
    }

    public void resetPassword(String token, String newPassword) {
        if (!StringUtils.hasText(token)) {
            throw new ServiceException(400, "reset_token_required");
        }
        String key = resetKey(token);
        String userIdValue = redisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(userIdValue)) {
            throw new ServiceException(400, "invalid_or_expired_reset_token");
        }
        Long userId = parseUserId(userIdValue)
                .orElseThrow(() -> new ServiceException(400, "invalid_or_expired_reset_token"));
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            redisTemplate.delete(key);
            throw new ServiceException(404, "user_not_found");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        usersMapper.updateById(user);
        redisTemplate.delete(key);
    }

    private void sendResetEmail(String to, String token) {
        String subject = forgotPasswordProperties.getEmailSubject();
        String link = buildResetLink(token);
        String body = "Use the link below to reset your password:\n" + link + "\n\n"
                + "If you did not request this, please ignore this email.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (StringUtils.hasText(subject)) {
            message.setSubject(subject);
        } else {
            message.setSubject("Password reset");
        }
        if (StringUtils.hasText(forgotPasswordProperties.getEmailFrom())) {
            message.setFrom(forgotPasswordProperties.getEmailFrom());
        }
        message.setText(body);
        mailSender.send(message);
    }

    private String buildResetLink(String token) {
        String template = forgotPasswordProperties.getResetUrlTemplate();
        if (StringUtils.hasText(template)) {
            if (template.contains("{token}")) {
                return template.replace("{token}", token);
            }
            return template + token;
        }
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private Optional<Long> parseUserId(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String resetKey(String token) {
        return RESET_KEY_PREFIX + token;
    }
}
