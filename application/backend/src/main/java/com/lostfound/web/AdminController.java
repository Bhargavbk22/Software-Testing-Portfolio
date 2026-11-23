package com.lostfound.web;

import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.AdminService;
import com.lostfound.service.ItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Administration")
public class AdminController {
    private final AdminService admin;
    private final ItemService items;

    @GetMapping("/dashboard")
    public ApiResponse<DashboardResponse> dashboard() { return ApiResponse.ok(admin.dashboard()); }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(@RequestParam(required = false) String q, @RequestParam(required = false) String role,
                                                         @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(admin.users(q, role, page, size));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<UserResponse> setStatus(@PathVariable Long id, @RequestBody StatusToggleRequest r) {
        if (r.enabled() == null) throw new ValidationFailedException(Map.of("enabled", "enabled is required"));
        return ApiResponse.ok("User updated", admin.setEnabled(id, r.enabled(), CurrentUser.get()));
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ItemResponse>> reports(@RequestParam(defaultValue = "FOUND") String type, @RequestParam(required = false) String q,
                                                           @RequestParam(required = false) String status,
                                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(items.search(type, q, null, null, null, null, status, false, CurrentUser.get(), page, size, "recent"));
    }

    @GetMapping("/activity-logs")
    public ApiResponse<PageResponse<ActivityLogResponse>> logs(@RequestParam(defaultValue = "false") boolean suspicious,
                                                               @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(admin.logs(suspicious, page, size));
    }

    @PostMapping("/categories")
    public ApiResponse<CategoryResponse> createCategory(@RequestBody CategoryRequest r) { return ApiResponse.ok("Category created", admin.createCategory(r, CurrentUser.get())); }

    @PutMapping("/categories/{id}")
    public ApiResponse<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest r) { return ApiResponse.ok("Category updated", admin.updateCategory(id, r, CurrentUser.get())); }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        admin.deleteCategory(id, CurrentUser.get());
        return new ApiResponse<>(true, "Category deleted", null, null, java.time.Instant.now());
    }
}
