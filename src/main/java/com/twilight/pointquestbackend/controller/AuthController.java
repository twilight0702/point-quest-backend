package com.twilight.pointquestbackend.controller;

import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.config.JwtProperties;
import com.twilight.pointquestbackend.dto.AuthResponse;
import com.twilight.pointquestbackend.dto.LoginRequest;
import com.twilight.pointquestbackend.dto.RegisterRequest;
import com.twilight.pointquestbackend.dto.UserProfileDto;
import com.twilight.pointquestbackend.security.JwtService;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtService jwtService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserPrincipal principal = authService.register(request);
        String token = jwtService.generateToken(principal);
        UserProfileDto profile = new UserProfileDto(principal.getUsername(), principal.getEmail(), principal.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(ApiResponse.success("register_success", new AuthResponse(token, profile)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        UserPrincipal principal = authService.login(request);
        String token = jwtService.generateToken(principal);
        UserProfileDto profile = new UserProfileDto(principal.getUsername(), principal.getEmail(), principal.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(ApiResponse.success("login_success", new AuthResponse(token, profile)));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<AuthResponse>> adminLogin(@Valid @RequestBody LoginRequest request) {
        UserPrincipal principal = authService.loginAdmin(request);
        String token = jwtService.generateToken(principal);
        UserProfileDto profile = new UserProfileDto(principal.getUsername(), principal.getEmail(), principal.getRole());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(ApiResponse.success("login_success", new AuthResponse(token, profile)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok()
                .body(ApiResponse.success("logout_success", null));
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileDto> me(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ServiceException(401, "unauthorized");
        }
        return authService.findUserProfile(principal.getId())
                .map(profile -> ApiResponse.success(new UserProfileDto(profile.getUsername(), profile.getEmail(), profile.getRole())))
                .orElseThrow(() -> new ServiceException(401, "unauthorized"));
    }
}
