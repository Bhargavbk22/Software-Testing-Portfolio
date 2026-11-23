package com.lostfound.web;

import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.AuthService;
import com.lostfound.service.ValidationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Authentication & Profile")
public class AuthController {
    private final AuthService auth;
    private final ValidationService validation;

    @PostMapping("/api/auth/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@RequestBody RegisterRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registration successful", auth.register(r)));
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest r) {
        return ApiResponse.ok("Login successful", auth.login(r));
    }

    /** Placeholder flow: always answers generically so account existence is never revealed. E-mail delivery is out of scope. */
    @PostMapping("/api/auth/forgot-password")
    public ApiResponse<Void> forgot(@RequestBody ForgotPasswordRequest r) {
        String err = validation.emailError(r.email());
        if (err != null) throw new ValidationFailedException(Map.of("email", err));
        return new ApiResponse<>(true, "If an account exists for this email, password reset instructions have been sent.", null, null, java.time.Instant.now());
    }

    @GetMapping("/api/users/profile")
    public ApiResponse<UserResponse> profile() { return ApiResponse.ok(UserResponse.from(CurrentUser.get())); }

    @PutMapping("/api/users/profile")
    public ApiResponse<UserResponse> update(@RequestBody UpdateProfileRequest r) {
        return ApiResponse.ok("Profile updated", auth.updateProfile(CurrentUser.get(), r));
    }
}
