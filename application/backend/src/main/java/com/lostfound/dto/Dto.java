package com.lostfound.dto;

import com.lostfound.domain.User;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response DTOs. Entities are never exposed directly. */
public final class Dto {
    private Dto() {}

    public record RegisterRequest(String fullName, String email, String password, String confirmPassword,
                                  String studentId, String department, String phone) {}
    public record LoginRequest(String email, String password) {}
    public record ForgotPasswordRequest(String email) {}
    public record UpdateProfileRequest(String fullName, String department, String phone) {}
    public record AuthResponse(String token, String tokenType, UserResponse user) {}

    public record UserResponse(Long id, String fullName, String email, String studentId, String department,
                               String phone, boolean enabled, List<String> roles, Instant createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getStudentId(), u.getDepartment(),
                    u.getPhone(), u.isEnabled(), u.getRoles().stream().map(r -> r.getName().name()).sorted().toList(), u.getCreatedAt());
        }
    }

    public record ItemRequest(String name, Long categoryId, String description, LocalDate date, String location,
                              String color, String brand, String identifyingCharacteristics,
                              String contactInfo, String handoverLocation) {}

    @Builder
    public record ItemResponse(Long id, String type, String name, Long categoryId, String categoryName, String description,
                               LocalDate date, String location, String color, String brand,
                               String identifyingCharacteristics, String contactInfo, String handoverLocation,
                               String status, boolean verified, List<String> images, Long reporterId, String reporterName,
                               Instant createdAt, List<MatchResponse> matches, List<StatusHistoryResponse> history) {}

    public record MatchResponse(Long id, int score, String status, ItemResponse lostItem, ItemResponse foundItem) {}
    public record StatusHistoryResponse(String fromStatus, String toStatus, String changedBy, String note, Instant at) {}

    public record ClaimRequest(Long foundItemId, String ownershipExplanation, String distinguishingCharacteristics,
                               String additionalDetails, List<String> evidence) {}
    public record DecisionRequest(Boolean ownershipVerified, String reason) {}
    public record MessageRequest(String message) {}
    public record ReasonRequest(String reason) {}
    public record StatusToggleRequest(Boolean enabled) {}

    @Builder
    public record ClaimResponse(Long id, Long foundItemId, String foundItemName, String foundItemStatus, Long claimantId,
                                String claimantName, String claimantEmail, String ownershipExplanation,
                                String distinguishingCharacteristics, String additionalDetails, List<String> evidence,
                                String status, boolean ownershipVerified, String decisionReason,
                                String foundItemIdentifyingCharacteristics, Instant createdAt, Instant updatedAt) {}

    public record RecoveryRequest(Long claimId, LocalDateTime scheduledAt, String location) {}
    public record RecoveryResponse(Long id, Long claimId, String itemName, String claimantName, String status,
                                   LocalDateTime scheduledAt, Instant handoverDate, String location, String handledBy,
                                   String confirmationCode) {}

    public record NotificationResponse(Long id, String type, String message, boolean read, String relatedType,
                                       Long relatedId, Instant createdAt) {}
    public record CategoryRequest(String name, String description) {}
    public record CategoryResponse(Long id, String name, String description) {}
    public record ActivityLogResponse(Long id, String user, String action, String entityType, Long entityId,
                                      String details, boolean suspicious, Instant createdAt) {}

    public record NameCount(String name, long value) {}
    public record MonthCount(String month, long lost, long found) {}
    public record DashboardResponse(long totalUsers, long totalLost, long totalFound, long pendingClaims,
                                    long approvedClaims, long recoveredItems, List<NameCount> lostVsFound,
                                    List<MonthCount> monthlyReports, List<NameCount> claimsByStatus,
                                    List<NameCount> categories) {}
    public record ModeratorDashboard(List<ItemResponse> pendingLost, List<ItemResponse> pendingFound,
                                     List<ItemResponse> awaitingVerification, List<MatchResponse> suggestedMatches,
                                     List<ClaimResponse> claimsToReview, List<ItemResponse> recentlyReviewed) {}
}
