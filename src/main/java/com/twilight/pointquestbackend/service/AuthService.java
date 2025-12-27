package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.Users;
import com.twilight.pointquestbackend.dto.LoginRequest;
import com.twilight.pointquestbackend.dto.RegisterRequest;
import com.twilight.pointquestbackend.dto.UserProfileDto;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.UsersMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class AuthService {
    private final UsersMapper usersMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsersMapper usersMapper, PointAccountMapper pointAccountMapper, PasswordEncoder passwordEncoder) {
        this.usersMapper = usersMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserPrincipal register(RegisterRequest request) {
        ensureEmailAvailable(request.getEmail());
        Users newUser = new Users();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRole("USER");
        Date now = new Date();
        newUser.setCreatedAt(now);
        newUser.setUpdatedAt(now);
        int inserted = usersMapper.insert(newUser);
        if (inserted != 1 || newUser.getId() == null) {
            throw new ServiceException(500, "failed_to_create_user");
        }
        createPointAccountForUser(newUser.getId(), now);
        return new UserPrincipal(newUser.getId(), newUser.getUsername(), newUser.getEmail(), newUser.getRole());
    }

    public UserPrincipal login(LoginRequest request) {
        Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>()
                .eq(Users::getEmail, request.getEmail()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(401, "invalid_credentials");
        }
        return new UserPrincipal(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public UserPrincipal loginAdmin(LoginRequest request) {
        UserPrincipal principal = login(request);
        if (!"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new ServiceException(403, "forbidden");
        }
        return principal;
    }

    public Optional<UserProfileDto> findUserProfile(Long userId) {
        Users user = usersMapper.selectById(userId);
        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(new UserProfileDto(user.getUsername(), user.getEmail(), user.getRole()));
    }

    private void ensureEmailAvailable(String email) {
        Long count = usersMapper.selectCount(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
        if (count != null && count > 0) {
            throw new ServiceException(409, "email_exists");
        }
    }

    private void createPointAccountForUser(Long userId, Date now) {
        PointAccount account = new PointAccount();
        account.setUser_id(userId);
        account.setBalance(0L);
        account.setUpdated_at(now);
        pointAccountMapper.insert(account);
    }
}
