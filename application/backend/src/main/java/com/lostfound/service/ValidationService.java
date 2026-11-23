package com.lostfound.service;

import com.lostfound.config.ValidationProperties;
import com.lostfound.dto.Dto.*;
import com.lostfound.exception.ValidationFailedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * All input rules. Every *Error method returns null when the value is valid, otherwise a user-facing message.
 * Limits come from {@link ValidationProperties}, which makes boundary values configurable and testable.
 */
@Service
public class ValidationService {
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$");
    private final ValidationProperties p;
    private final Clock clock;

    public ValidationService(ValidationProperties p, Clock clock) {
        this.p = p;
        this.clock = clock;
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    public String emailError(String e) {
        if (blank(e)) return "Email is required";
        String t = e.trim();
        if (t.length() > 254 || !EMAIL.matcher(t).matches()) return "Email format is invalid";
        return null;
    }

    public String passwordError(String pw) {
        if (pw == null || pw.isEmpty()) return "Password is required";
        if (pw.length() < p.getPasswordMinLength()) return "Password must be at least " + p.getPasswordMinLength() + " characters";
        if (pw.length() > p.getPasswordMaxLength()) return "Password must be at most " + p.getPasswordMaxLength() + " characters";
        if (!pw.matches(".*[A-Z].*")) return "Password must contain an uppercase letter";
        if (!pw.matches(".*[a-z].*")) return "Password must contain a lowercase letter";
        if (!pw.matches(".*[0-9].*")) return "Password must contain a digit";
        if (!pw.matches(".*[^A-Za-z0-9].*")) return "Password must contain a special character";
        return null;
    }

    public String phoneError(String ph) {
        if (blank(ph)) return "Phone number is required";
        return ph.trim().matches(p.getPhonePattern()) ? null : "Phone number must contain 10 to 15 digits (optional leading +)";
    }

    public String studentIdError(String id) {
        if (blank(id)) return "Student ID is required";
        return id.trim().matches(p.getStudentIdPattern()) ? null : "Student ID must be 5 to 20 letters or digits";
    }

    public String textError(String v, String label, int min, int max) {
        if (blank(v)) return label + " is required";
        int len = v.trim().length();
        if (len < min) return label + " must be at least " + min + " characters";
        if (len > max) return label + " must be at most " + max + " characters";
        return null;
    }

    public String itemNameError(String n) { return textError(n, "Item name", p.getItemNameMinLength(), p.getItemNameMaxLength()); }
    public String descriptionError(String d) { return textError(d, "Description", p.getDescriptionMinLength(), p.getDescriptionMaxLength()); }

    public String dateError(LocalDate d) {
        if (d == null) return "Date is required";
        LocalDate today = LocalDate.now(clock);
        if (d.isAfter(today)) return "Date cannot be in the future";
        if (d.isBefore(today.minusDays(p.getDateMaxAgeDays()))) return "Date is too far in the past (max " + p.getDateMaxAgeDays() + " days)";
        return null;
    }

    private void put(Map<String, String> errors, String field, String message) { if (message != null) errors.put(field, message); }

    private void throwIfAny(Map<String, String> errors) { if (!errors.isEmpty()) throw new ValidationFailedException(errors); }

    public void validateRegistration(RegisterRequest r) {
        Map<String, String> e = new LinkedHashMap<>();
        put(e, "fullName", textError(r.fullName(), "Full name", p.getNameMinLength(), p.getNameMaxLength()));
        put(e, "email", emailError(r.email()));
        put(e, "password", passwordError(r.password()));
        if (r.confirmPassword() == null || r.confirmPassword().isEmpty()) e.put("confirmPassword", "Confirm password is required");
        else if (!r.confirmPassword().equals(r.password())) e.put("confirmPassword", "Passwords do not match");
        put(e, "studentId", studentIdError(r.studentId()));
        if (blank(r.department())) e.put("department", "Department is required");
        put(e, "phone", phoneError(r.phone()));
        throwIfAny(e);
    }

    public void validateItem(ItemRequest r, boolean found) {
        Map<String, String> e = new LinkedHashMap<>();
        put(e, "name", itemNameError(r.name()));
        if (r.categoryId() == null) e.put("categoryId", "Category is required");
        put(e, "description", descriptionError(r.description()));
        put(e, "date", dateError(r.date()));
        if (blank(r.location())) e.put("location", "Location is required");
        else if (r.location().trim().length() > p.getLocationMaxLength()) e.put("location", "Location must be at most " + p.getLocationMaxLength() + " characters");
        if (r.color() != null && r.color().length() > 50) e.put("color", "Color must be at most 50 characters");
        if (r.brand() != null && r.brand().length() > 50) e.put("brand", "Brand must be at most 50 characters");
        if (r.identifyingCharacteristics() != null && r.identifyingCharacteristics().length() > 1000) e.put("identifyingCharacteristics", "Must be at most 1000 characters");
        if (found) { if (blank(r.handoverLocation())) e.put("handoverLocation", "Handover location is required"); }
        else if (blank(r.contactInfo())) e.put("contactInfo", "Contact information is required");
        throwIfAny(e);
    }

    public void validateClaim(ClaimRequest r) {
        Map<String, String> e = new LinkedHashMap<>();
        if (r.foundItemId() == null) e.put("foundItemId", "Item being claimed is required");
        put(e, "ownershipExplanation", textError(r.ownershipExplanation(), "Ownership explanation", p.getClaimExplanationMinLength(), p.getClaimExplanationMaxLength()));
        if (blank(r.distinguishingCharacteristics())) e.put("distinguishingCharacteristics", "Distinguishing characteristics are required");
        throwIfAny(e);
    }

    public void validateImage(MultipartFile f, int existingCount) {
        if (f == null || f.isEmpty()) throw new ValidationFailedException(Map.of("file", "Image file is required"));
        validateImage(f.getContentType(), f.getOriginalFilename(), f.getSize(), existingCount);
    }

    public void validateImage(String contentType, String filename, long size, int existingCount) {
        Map<String, String> e = new LinkedHashMap<>();
        String ext = filename != null && filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (contentType == null || !p.getAllowedImageTypes().contains(contentType.toLowerCase(Locale.ROOT))
                || !p.getAllowedImageExtensions().contains(ext)) {
            e.put("file", "Unsupported image format. Allowed: " + String.join(", ", p.getAllowedImageExtensions()));
        } else if (size <= 0) {
            e.put("file", "Image file is empty");
        } else if (size > p.getImageMaxSizeBytes()) {
            e.put("file", "Image must not exceed " + (p.getImageMaxSizeBytes() / 1024) + " KB");
        } else if (existingCount >= p.getMaxImagesPerItem()) {
            e.put("file", "A maximum of " + p.getMaxImagesPerItem() + " images is allowed per item");
        }
        throwIfAny(e);
    }
}
