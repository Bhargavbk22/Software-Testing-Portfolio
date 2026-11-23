package com.lostfound.web;

import com.lostfound.domain.Enums.MatchStatus;
import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.Mapper;
import com.lostfound.service.MatchService;
import com.lostfound.service.ModeratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/moderator")
@RequiredArgsConstructor
@Tag(name = "Moderation")
public class ModeratorController {
    private final ModeratorService moderation;
    private final MatchService matching;
    private final Mapper mapper;

    @GetMapping("/dashboard")
    public ApiResponse<ModeratorDashboard> dashboard() { return ApiResponse.ok(moderation.dashboard(CurrentUser.get())); }

    @PutMapping("/reports/{type}/{id}/review")
    public ApiResponse<ItemResponse> review(@PathVariable String type, @PathVariable Long id) { return ApiResponse.ok("Review started", moderation.review(type, id, CurrentUser.get())); }

    @PutMapping("/reports/{type}/{id}/approve")
    public ApiResponse<ItemResponse> approve(@PathVariable String type, @PathVariable Long id) { return ApiResponse.ok("Report approved", moderation.approve(type, id, CurrentUser.get())); }

    @PutMapping("/reports/{type}/{id}/reject")
    public ApiResponse<ItemResponse> reject(@PathVariable String type, @PathVariable Long id, @RequestBody ReasonRequest r) {
        return ApiResponse.ok("Report rejected", moderation.reject(type, id, r.reason(), CurrentUser.get()));
    }

    @GetMapping("/matches")
    public ApiResponse<PageResponse<MatchResponse>> matches(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        MatchStatus st = null;
        if (status != null && !status.isBlank()) {
            try { st = MatchStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException e) { throw new ValidationFailedException(Map.of("status", "Unknown status: " + status)); }
        }
        return ApiResponse.ok(PageResponse.of(matching.list(st, page, size), mapper::match));
    }

    @PutMapping("/matches/{id}/confirm")
    public ApiResponse<MatchResponse> confirm(@PathVariable Long id) { return ApiResponse.ok("Match confirmed", mapper.match(matching.confirm(id, CurrentUser.get()))); }

    @PutMapping("/matches/{id}/dismiss")
    public ApiResponse<MatchResponse> dismiss(@PathVariable Long id) { return ApiResponse.ok("Match dismissed", mapper.match(matching.dismiss(id, CurrentUser.get()))); }
}
