package com.lostfound.web;

import com.lostfound.domain.Enums.ClaimStatus;
import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.ClaimService;
import com.lostfound.service.Mapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Claims")
public class ClaimController {
    private final ClaimService claims;
    private final Mapper mapper;

    @PostMapping("/api/claims")
    public ResponseEntity<ApiResponse<ClaimResponse>> submit(@RequestBody ClaimRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Claim submitted", claims.submit(r, CurrentUser.get())));
    }

    @GetMapping("/api/claims/my-claims")
    public ApiResponse<PageResponse<ClaimResponse>> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(PageResponse.of(claims.myClaims(CurrentUser.get(), page, size), c -> mapper.claim(c, false)));
    }

    @GetMapping("/api/claims/{id}")
    public ApiResponse<ClaimResponse> get(@PathVariable Long id) { return ApiResponse.ok(claims.get(id, CurrentUser.get())); }

    @PutMapping("/api/claims/{id}/cancel")
    public ApiResponse<ClaimResponse> cancel(@PathVariable Long id) { return ApiResponse.ok("Claim cancelled", claims.cancel(id, CurrentUser.get())); }

    @PostMapping("/api/claims/{id}/evidence")
    public ApiResponse<ClaimResponse> evidence(@PathVariable Long id, @RequestBody MessageRequest r) {
        return ApiResponse.ok("Evidence added", claims.addEvidence(id, r.message(), CurrentUser.get()));
    }

    // ---- admin ----
    @GetMapping("/api/admin/claims")
    public ApiResponse<PageResponse<ClaimResponse>> adminList(@RequestParam(required = false) String status,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        ClaimStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = ClaimStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new ValidationFailedException(Map.of("status", "Unknown status: " + status)); }
        }
        return ApiResponse.ok(PageResponse.of(claims.adminList(st, page, size), c -> mapper.claim(c, true)));
    }

    @PutMapping("/api/admin/claims/{id}/review")
    public ApiResponse<ClaimResponse> review(@PathVariable Long id) { return ApiResponse.ok("Review started", claims.startReview(id, CurrentUser.get())); }

    @PutMapping("/api/admin/claims/{id}/approve")
    public ApiResponse<ClaimResponse> approve(@PathVariable Long id, @RequestBody DecisionRequest d) {
        return ApiResponse.ok("Claim approved", claims.approve(id, d, CurrentUser.get()));
    }

    @PutMapping("/api/admin/claims/{id}/reject")
    public ApiResponse<ClaimResponse> reject(@PathVariable Long id, @RequestBody DecisionRequest d) {
        return ApiResponse.ok("Claim rejected", claims.reject(id, d, CurrentUser.get()));
    }

    @PutMapping("/api/admin/claims/{id}/request-evidence")
    public ApiResponse<ClaimResponse> requestEvidence(@PathVariable Long id, @RequestBody MessageRequest r) {
        return ApiResponse.ok("Evidence requested", claims.requestEvidence(id, r.message(), CurrentUser.get()));
    }

    // ---- moderator ----
    @GetMapping("/api/moderator/claims")
    public ApiResponse<java.util.List<ClaimResponse>> moderatorClaims() {
        return ApiResponse.ok(claims.needingReview().stream().map(c -> mapper.claim(c, true)).toList());
    }

    @PutMapping("/api/moderator/claims/{id}/escalate")
    public ApiResponse<ClaimResponse> escalate(@PathVariable Long id) { return ApiResponse.ok("Claim escalated to administrators", claims.startReview(id, CurrentUser.get())); }
}
