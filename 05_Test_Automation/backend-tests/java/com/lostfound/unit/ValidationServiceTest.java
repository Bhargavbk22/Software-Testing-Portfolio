package com.lostfound.unit;

import com.lostfound.config.ValidationProperties;
import com.lostfound.dto.Dto.RegisterRequest;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Equivalence Partitioning + Boundary Value Analysis for every input rule. */
class ValidationServiceTest {
    private ValidationProperties props;
    private ValidationService v;
    private final LocalDate today = LocalDate.of(2026, 6, 15);

    @BeforeEach
    void setUp() {
        props = new ValidationProperties();
        v = new ValidationService(props, Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC));
    }

    // ---------- Email (EP) ----------
    @ParameterizedTest(name = "valid email partition: {0}")
    @ValueSource(strings = {"a@b.com", "user.name+tag@campus.edu", "x_y@sub.domain.org"})
    void email_validPartition_accepted(String email) { assertNull(v.emailError(email)); }

    @ParameterizedTest(name = "invalid email partition: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "plain", "@no.com", "a@b", "a b@c.com", "a@b..com"})
    void email_invalidPartition_rejected(String email) { assertNotNull(v.emailError(email)); }

    // ---------- Password (EP + BVA on min=8, max=64) ----------
    @Test @DisplayName("Password BVA: 7 chars rejected, 8 accepted")
    void password_lowerBoundary() {
        assertNotNull(v.passwordError("Abcd12!"));
        assertNull(v.passwordError("Abcd123!"));
        assertNull(v.passwordError("Abcd1234!"));
    }

    @Test @DisplayName("Password BVA: 64 chars accepted, 65 rejected")
    void password_upperBoundary() {
        String p64 = "Aa1!" + "a".repeat(60);
        assertEquals(64, p64.length());
        assertNull(v.passwordError(p64));
        assertNotNull(v.passwordError(p64 + "a"));
    }

    @ParameterizedTest(name = "weak password partition: [{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {"alllowercase1!", "ALLUPPERCASE1!", "NoDigitsHere!", "NoSpecial123A"})
    void password_complexityPartitions_rejected(String pw) { assertNotNull(v.passwordError(pw)); }

    @Test @DisplayName("Password boundary follows configured limits")
    void password_limitsAreConfigurable() {
        props.setPasswordMinLength(12);
        assertNotNull(v.passwordError("Abcd123!"));
        assertNull(v.passwordError("Abcd1234567!"));
    }

    // ---------- Phone (EP) ----------
    @ParameterizedTest @ValueSource(strings = {"9876543210", "+919876543210", "123456789012345"})
    void phone_valid(String p) { assertNull(v.phoneError(p)); }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"12345", "98765abcde", "1234567890123456", "++919876543210"})
    void phone_invalid(String p) { assertNotNull(v.phoneError(p)); }

    // ---------- Item name (BVA 2..100) ----------
    @Test
    void itemName_boundaries() {
        assertNotNull(v.itemNameError("a"));
        assertNull(v.itemNameError("ab"));
        assertNull(v.itemNameError("a".repeat(100)));
        assertNotNull(v.itemNameError("a".repeat(101)));
        assertNotNull(v.itemNameError("   "));
    }

    // ---------- Description (BVA 10..1000) ----------
    @Test
    void description_boundaries() {
        assertNotNull(v.descriptionError("a".repeat(9)));
        assertNull(v.descriptionError("a".repeat(10)));
        assertNull(v.descriptionError("a".repeat(1000)));
        assertNotNull(v.descriptionError("a".repeat(1001)));
        assertNotNull(v.descriptionError(null));
    }

    // ---------- Dates (BVA: today, tomorrow, max age) ----------
    @Test
    void date_boundaries() {
        assertNull(v.dateError(today));
        assertEquals("Date cannot be in the future", v.dateError(today.plusDays(1)));
        assertNull(v.dateError(today.minusDays(730)));
        assertNotNull(v.dateError(today.minusDays(731)));
        assertNotNull(v.dateError(null));
    }

    // ---------- Images (EP + BVA) ----------
    @Test
    void image_sizeBoundary() {
        long max = props.getImageMaxSizeBytes();
        assertDoesNotThrow(() -> v.validateImage("image/png", "a.png", max, 0));
        assertThrows(ValidationFailedException.class, () -> v.validateImage("image/png", "a.png", max + 1, 0));
        assertThrows(ValidationFailedException.class, () -> v.validateImage("image/png", "a.png", 0, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/gif", "application/pdf", "text/plain"})
    void image_unsupportedTypes_rejected(String type) {
        assertThrows(ValidationFailedException.class, () -> v.validateImage(type, "file.png", 100, 0));
    }

    @Test
    void image_extensionMustMatchAllowedList() {
        assertThrows(ValidationFailedException.class, () -> v.validateImage("image/png", "virus.exe", 100, 0));
        assertDoesNotThrow(() -> v.validateImage("image/jpeg", "photo.JPG", 100, 0));
    }

    @Test
    void image_maxCountBoundary() {
        assertDoesNotThrow(() -> v.validateImage("image/png", "a.png", 100, props.getMaxImagesPerItem() - 1));
        assertThrows(ValidationFailedException.class, () -> v.validateImage("image/png", "a.png", 100, props.getMaxImagesPerItem()));
    }

    // ---------- Registration ----------
    @Test
    void registration_valid_noException() {
        assertDoesNotThrow(() -> v.validateRegistration(new RegisterRequest("Ann Lee", "ann@campus.edu", "Passw0rd!", "Passw0rd!", "STU12345", "CS", "9876543210")));
    }

    @Test
    void registration_missingFields_reportsEveryField() {
        ValidationFailedException ex = assertThrows(ValidationFailedException.class,
                () -> v.validateRegistration(new RegisterRequest("", "", "", "", "", "", "")));
        assertTrue(ex.getErrors().keySet().containsAll(java.util.List.of("fullName", "email", "password", "confirmPassword", "studentId", "department", "phone")));
    }

    @Test
    void registration_passwordMismatch_rejected() {
        ValidationFailedException ex = assertThrows(ValidationFailedException.class,
                () -> v.validateRegistration(new RegisterRequest("Ann Lee", "ann@campus.edu", "Passw0rd!", "Other0rd!", "STU12345", "CS", "9876543210")));
        assertEquals("Passwords do not match", ex.getErrors().get("confirmPassword"));
    }
}
