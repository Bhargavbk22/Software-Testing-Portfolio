package com.lostfound.web;

import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.Mapper;
import com.lostfound.service.RecoveryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recoveries")
@RequiredArgsConstructor
@Tag(name = "Recovery")
public class RecoveryController {
    private final RecoveryService recoveries;
    private final Mapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<RecoveryResponse>> schedule(@RequestBody RecoveryRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Handover scheduled", recoveries.schedule(r, CurrentUser.get())));
    }

    @GetMapping
    public ApiResponse<PageResponse<RecoveryResponse>> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(PageResponse.of(recoveries.list(page, size), mapper::recovery));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<RecoveryResponse>> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(PageResponse.of(recoveries.mine(CurrentUser.get(), page, size), mapper::recovery));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<RecoveryResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok("Item marked as returned", recoveries.complete(id, CurrentUser.get()));
    }
}
