package com.lostfound.unit;

import com.lostfound.config.ValidationProperties;
import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.Role;
import com.lostfound.domain.User;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.DuplicateResourceException;
import com.lostfound.exception.UnauthorizedException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.RoleRepository;
import com.lostfound.repo.UserRepository;
import com.lostfound.security.JwtService;
import com.lostfound.service.ActivityService;
import com.lostfound.service.AuthService;
import com.lostfound.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock RoleRepository roles;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @Mock ActivityService activity;
    AuthService service;

    private final RegisterRequest valid = new RegisterRequest("Ann Lee", "Ann@Campus.edu", "Passw0rd!", "Passw0rd!", "STU12345", "CS", "9876543210");

    @BeforeEach
    void setUp() {
        service = new AuthService(users, roles, encoder, jwt, new ValidationService(new ValidationProperties(), Clock.systemDefaultZone()), activity);
        when(roles.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(new Role(RoleName.ROLE_USER)));
        when(encoder.encode(any())).thenReturn("hashed");
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void register_validRequest_createsUserWithHashedPasswordAndUserRole() {
        UserResponse res = service.register(valid);
        assertEquals("ann@campus.edu", res.email());
        assertEquals(java.util.List.of("ROLE_USER"), res.roles());
        verify(users).save(argThat(u -> "hashed".equals(u.getPasswordHash())));
    }

    @Test
    void register_duplicateEmail_isRejected() {
        when(users.existsByEmailIgnoreCase("ann@campus.edu")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(valid));
        verify(users, never()).save(any());
    }

    @Test
    void register_duplicateStudentId_isRejected() {
        when(users.existsByStudentId("STU12345")).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.register(valid));
    }

    @Test
    void register_invalidInput_isRejectedBeforeAnyLookup() {
        assertThrows(ValidationFailedException.class, () -> service.register(new RegisterRequest("", "bad", "x", "y", "", "", "")));
        verifyNoInteractions(users);
    }

    @Test
    void login_validCredentials_returnsToken() {
        User u = TestData.user(1, RoleName.ROLE_USER);
        u.setEnabled(true);
        when(users.findByEmailIgnoreCase("user1@campus.edu")).thenReturn(Optional.of(u));
        when(encoder.matches("Passw0rd!", "hash")).thenReturn(true);
        when(jwt.generate(u)).thenReturn("jwt-token");
        AuthResponse res = service.login(new LoginRequest("user1@campus.edu", "Passw0rd!"));
        assertEquals("jwt-token", res.token());
        assertEquals("Bearer", res.tokenType());
    }

    @Test
    void login_wrongPassword_isRejectedAndFlaggedSuspicious() {
        User u = TestData.user(1, RoleName.ROLE_USER);
        when(users.findByEmailIgnoreCase("user1@campus.edu")).thenReturn(Optional.of(u));
        when(encoder.matches(any(), any())).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("user1@campus.edu", "wrong")));
        verify(activity).log(any(), eq("LOGIN_FAILED"), any(), any(), any(), eq(true));
    }

    @Test
    void login_unknownEmail_isRejected() {
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("nobody@campus.edu", "Passw0rd!")));
    }

    @Test
    void login_disabledAccount_isRejected() {
        User u = TestData.user(1, RoleName.ROLE_USER);
        u.setEnabled(false);
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.of(u));
        when(encoder.matches(any(), any())).thenReturn(true);
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("user1@campus.edu", "Passw0rd!")));
    }

    @Test
    void login_missingFields_failValidation() {
        assertThrows(ValidationFailedException.class, () -> service.login(new LoginRequest("", "")));
    }
}
