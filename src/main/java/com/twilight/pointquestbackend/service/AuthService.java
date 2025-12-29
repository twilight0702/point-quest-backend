package com.twilight.pointquestbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.twilight.pointquestbackend.common.ServiceException;
import com.twilight.pointquestbackend.common.UserType;
import com.twilight.pointquestbackend.domain.AdminUser;
import com.twilight.pointquestbackend.domain.PointAccount;
import com.twilight.pointquestbackend.domain.Users;
import com.twilight.pointquestbackend.dto.AdminLoginDTO;
import com.twilight.pointquestbackend.dto.LoginDTO;
import com.twilight.pointquestbackend.dto.RegisterDTO;
import com.twilight.pointquestbackend.mapper.AdminUserMapper;
import com.twilight.pointquestbackend.mapper.PointAccountMapper;
import com.twilight.pointquestbackend.mapper.UsersMapper;
import com.twilight.pointquestbackend.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AdminUserMapper adminUserMapper;
    private final UsersMapper usersMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AdminUserMapper adminUserMapper, UsersMapper usersMapper, PointAccountMapper pointAccountMapper, PasswordEncoder passwordEncoder) {
        this.adminUserMapper = adminUserMapper;
        this.usersMapper = usersMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public UserPrincipal register(RegisterDTO request) {
        ensureEmailAvailable(request.getEmail());
        Users newUser = new Users();
        newUser.setUsername(request.getUsername());
        newUser.setEmail(request.getEmail());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        int inserted = usersMapper.insert(newUser);
        if (inserted != 1 || newUser.getId() == null) {
            throw new ServiceException(500, "failed_to_create_user");
        }
        createPointAccountForUser(newUser.getId());
        return new UserPrincipal(newUser.getId(), newUser.getUsername(), newUser.getEmail(), UserType.USER.getName());
    }

    public UserPrincipal login(LoginDTO request) {
        Users user = usersMapper.selectOne(new LambdaQueryWrapper<Users>()
                .eq(Users::getEmail, request.getEmail()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ServiceException(401, "invalid_credentials");
        }
        return new UserPrincipal(user.getId(), user.getUsername(), user.getEmail(), UserType.USER.getName());
    }

    public UserPrincipal loginAdmin(AdminLoginDTO request) {
        AdminUser admin = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername()));
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new ServiceException(401, "invalid_credentials");
        }
        return new UserPrincipal(admin.getId(), admin.getUsername(), null, UserType.ADMIN.getName());
    }

    private void ensureEmailAvailable(String email) {
        Long count = usersMapper.selectCount(new LambdaQueryWrapper<Users>().eq(Users::getEmail, email));
        if (count != null && count > 0) {
            throw new ServiceException(409, "email_exists");
        }
    }

    private void createPointAccountForUser(Long userId) {
        PointAccount account = new PointAccount();
        account.setUserId(userId);
        account.setBalance(0L);
        pointAccountMapper.insert(account);
    }
}
