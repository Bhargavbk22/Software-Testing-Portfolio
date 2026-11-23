package com.lostfound.web;

import com.lostfound.dto.ApiResponse;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.security.CurrentUser;
import com.lostfound.service.AdminService;
import com.lostfound.service.ItemService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Items, Search & Categories")
public class ItemController {
    private final ItemService items;
    private final AdminService admin;

    // ---- lost ----
    @PostMapping("/api/lost-items")
    public ResponseEntity<ApiResponse<ItemResponse>> createLost(@RequestBody ItemRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Lost report submitted", items.createLost(r, CurrentUser.get())));
    }

    @GetMapping("/api/lost-items")
    public ApiResponse<PageResponse<ItemResponse>> listLost(@RequestParam(defaultValue = "true") boolean mine,
                                                            @RequestParam(required = false) String q,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(items.search("LOST", q, null, null, null, null, status, mine, CurrentUser.get(), page, size, "recent"));
    }

    @GetMapping("/api/lost-items/{id}")
    public ApiResponse<ItemResponse> getLost(@PathVariable Long id) { return ApiResponse.ok(items.getLost(id, CurrentUser.get())); }

    @PutMapping("/api/lost-items/{id}")
    public ApiResponse<ItemResponse> updateLost(@PathVariable Long id, @RequestBody ItemRequest r) {
        return ApiResponse.ok("Lost report updated", items.updateLost(id, r, CurrentUser.get()));
    }

    @DeleteMapping("/api/lost-items/{id}")
    public ApiResponse<ItemResponse> withdrawLost(@PathVariable Long id) {
        return ApiResponse.ok("Lost report withdrawn", items.withdrawLost(id, CurrentUser.get()));
    }

    @PostMapping(value = "/api/lost-items/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ItemResponse> lostImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Image uploaded", items.uploadImage("LOST", id, file, CurrentUser.get()));
    }

    // ---- found ----
    @PostMapping("/api/found-items")
    public ResponseEntity<ApiResponse<ItemResponse>> createFound(@RequestBody ItemRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Found report submitted", items.createFound(r, CurrentUser.get())));
    }

    @GetMapping("/api/found-items")
    public ApiResponse<PageResponse<ItemResponse>> listFound(@RequestParam(defaultValue = "true") boolean mine,
                                                             @RequestParam(required = false) String q,
                                                             @RequestParam(required = false) String status,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(items.search("FOUND", q, null, null, null, null, status, mine, CurrentUser.get(), page, size, "recent"));
    }

    @GetMapping("/api/found-items/{id}")
    public ApiResponse<ItemResponse> getFound(@PathVariable Long id) { return ApiResponse.ok(items.getFound(id, CurrentUser.get())); }

    @PutMapping("/api/found-items/{id}")
    public ApiResponse<ItemResponse> updateFound(@PathVariable Long id, @RequestBody ItemRequest r) {
        return ApiResponse.ok("Found report updated", items.updateFound(id, r, CurrentUser.get()));
    }

    @PostMapping(value = "/api/found-items/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ItemResponse> foundImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok("Image uploaded", items.uploadImage("FOUND", id, file, CurrentUser.get()));
    }

    // ---- search ----
    @GetMapping("/api/items/search")
    public ApiResponse<PageResponse<ItemResponse>> search(@RequestParam(defaultValue = "FOUND") String type,
                                                          @RequestParam(required = false) String q,
                                                          @RequestParam(required = false) Long categoryId,
                                                          @RequestParam(required = false) String location,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(defaultValue = "recent") String sort,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "9") int size) {
        return ApiResponse.ok(items.search(type, q, categoryId, location, from, to, status, false, CurrentUser.get(), page, size, sort));
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryResponse>> categories() { return ApiResponse.ok(admin.categories()); }
}
