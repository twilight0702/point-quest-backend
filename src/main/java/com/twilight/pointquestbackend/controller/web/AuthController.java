package com.twilight.pointquestbackend.controller.web;

import com.twilight.pointquestbackend.common.ApiResponse;
import com.twilight.pointquestbackend.dto.AdminLoginDTO;
import com.twilight.pointquestbackend.dto.ForgotPasswordRequestDTO;
import com.twilight.pointquestbackend.vo.auth.AuthVO;
import com.twilight.pointquestbackend.dto.LoginDTO;
import com.twilight.pointquestbackend.dto.ResetPasswordDTO;
import com.twilight.pointquestbackend.dto.RegisterDTO;
import com.twilight.pointquestbackend.security.JwtService;
import com.twilight.pointquestbackend.security.UserPrincipal;
import com.twilight.pointquestbackend.service.AuthService;
import com.twilight.pointquestbackend.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          PasswordResetService passwordResetService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthVO> register(@Valid @RequestBody RegisterDTO request) {
        UserPrincipal principal = authService.register(request);
        String token = jwtService.generateToken(principal);
        return ApiResponse.success("register_success", new AuthVO(token));
    }

    @PostMapping("/login")
    public ApiResponse<AuthVO> login(@Valid @RequestBody LoginDTO request) {
        UserPrincipal principal = authService.login(request);
        String token = jwtService.generateToken(principal);
        return ApiResponse.success("login_success", new AuthVO(token));
    }

    @PostMapping("/admin/login")
    public ApiResponse<AuthVO> adminLogin(@Valid @RequestBody AdminLoginDTO request) {
        UserPrincipal principal = authService.loginAdmin(request);
        String token = jwtService.generateToken(principal);
        return ApiResponse.success("login_success", new AuthVO(token));
    }

    @PostMapping("/forgot-password/request")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestReset(request.getEmail());
        return ApiResponse.onlySuccess("forgot_password_requested");
    }

    @PostMapping("/forgot-password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ApiResponse.onlySuccess("password_reset_success");
    }
}
