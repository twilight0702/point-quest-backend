package com.twilight.pointquestbackend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilight.pointquestbackend.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Unified security exception handler for unauthorized/forbidden responses.
 */
@Component
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        writeBody(response, HttpServletResponse.SC_UNAUTHORIZED, ApiResponse.failure(401, "unauthorized"));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        writeBody(response, HttpServletResponse.SC_FORBIDDEN, ApiResponse.failure(403, "forbidden"));
    }

    private void writeBody(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
