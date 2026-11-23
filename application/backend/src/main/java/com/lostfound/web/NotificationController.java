package com.lostfound.web;

import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.NotificationResponse;
import com.lostfound.dto.PageResponse;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.Mapper;
import com.lostfound.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final Mapper mapper;

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "15") int size) {
        return ApiResponse.ok(PageResponse.of(notifications.list(CurrentUser.get(), unreadOnly, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), mapper::notification));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unread() { return ApiResponse.ok(Map.of("count", notifications.unreadCount(CurrentUser.get()))); }

    @PutMapping("/{id}/read")
    public ApiResponse<NotificationResponse> read(@PathVariable Long id) { return ApiResponse.ok(mapper.notification(notifications.markRead(id, CurrentUser.get()))); }

    @PutMapping("/read-all")
    public ApiResponse<Map<String, Integer>> readAll() { return ApiResponse.ok(Map.of("updated", notifications.markAllRead(CurrentUser.get()))); }
}
