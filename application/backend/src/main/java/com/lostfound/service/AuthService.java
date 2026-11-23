package com.lostfound.service;

import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.Role;
import com.lostfound.domain.User;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.DuplicateResourceException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.exception.UnauthorizedException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.RoleRepository;
import com.lostfound.repo.UserRepository;
import com.lostfound.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final ValidationService validation;
    private final ActivityService activity;

    @Transactional
    public UserResponse register(RegisterRequest r) {
        validation.validateRegistration(r);
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("An account with this email already exists");
        if (users.existsByStudentId(r.studentId().trim())) throw new DuplicateResourceException("An account with this Student ID already exists");
        Role role = roles.findByName(RoleName.ROLE_USER).orElseThrow(() -> new ResourceNotFoundException("Default role is missing"));
        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setStudentId(r.studentId().trim());
        u.setDepartment(r.department().trim());
        u.setPhone(r.phone().trim());
        u.getRoles().add(role);
        users.save(u);
        activity.log(u, "USER_REGISTERED", "USER", u.getId(), u.getEmail());
        return UserResponse.from(u);
    }

    /** Deliberately not transactional so the failed-login audit entry is committed even though an exception follows. */
    public AuthResponse login(LoginRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.email() == null || r.email().isBlank()) errors.put("email", "Email is required");
        if (r.password() == null || r.password().isEmpty()) errors.put("password", "Password is required");
        if (!errors.isEmpty()) throw new ValidationFailedException(errors);
        User u = users.findByEmailIgnoreCase(r.email().trim()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            activity.log(u, "LOGIN_FAILED", "USER", u == null ? null : u.getId(), "Failed login for " + r.email(), true);
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!u.isEnabled()) throw new UnauthorizedException("This account has been disabled");
        activity.log(u, "LOGIN", "USER", u.getId(), null);
        return new AuthResponse(jwt.generate(u), "Bearer", UserResponse.from(u));
    }

    @Transactional
    public UserResponse updateProfile(User current, UpdateProfileRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        String n = validation.textError(r.fullName(), "Full name", 2, 100);
        if (n != null) errors.put("fullName", n);
        String ph = validation.phoneError(r.phone());
        if (ph != null) errors.put("phone", ph);
        if (r.department() == null || r.department().isBlank()) errors.put("department", "Department is required");
        if (!errors.isEmpty()) throw new ValidationFailedException(errors);
        User u = users.findById(current.getId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setFullName(r.fullName().trim());
        u.setDepartment(r.department().trim());
        u.setPhone(r.phone().trim());
        users.save(u);
        activity.log(u, "PROFILE_UPDATED", "USER", u.getId(), null);
        return UserResponse.from(u);
    }
}
