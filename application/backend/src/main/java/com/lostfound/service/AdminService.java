package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.DuplicateResourceException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository users;
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ClaimRepository claimRepo;
    private final CategoryRepository categories;
    private final ActivityLogRepository logs;
    private final ActivityService activity;
    private final Mapper mapper;

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        long lost = lostRepo.count(), found = foundRepo.count();
        long pending = claimRepo.countByStatus(ClaimStatus.PENDING) + claimRepo.countByStatus(ClaimStatus.UNDER_REVIEW);
        long approved = claimRepo.countByStatus(ClaimStatus.APPROVED) + claimRepo.countByStatus(ClaimStatus.HANDOVER_SCHEDULED);
        long recovered = foundRepo.countByStatus(FoundStatus.RETURNED);

        List<NameCount> claimStatuses = Arrays.stream(ClaimStatus.values()).map(s -> new NameCount(s.name(), claimRepo.countByStatus(s))).toList();

        Map<String, Long> cat = new TreeMap<>();
        lostRepo.findAll().forEach(i -> cat.merge(i.getCategory().getName(), 1L, Long::sum));
        foundRepo.findAll().forEach(i -> cat.merge(i.getCategory().getName(), 1L, Long::sum));

        YearMonth now = YearMonth.now();
        Map<YearMonth, long[]> months = new LinkedHashMap<>();
        for (int k = 5; k >= 0; k--) months.put(now.minusMonths(k), new long[2]);
        lostRepo.findAll().forEach(i -> bump(months, i.getCreatedAt(), 0));
        foundRepo.findAll().forEach(i -> bump(months, i.getCreatedAt(), 1));
        List<MonthCount> monthly = months.entrySet().stream().map(e -> new MonthCount(e.getKey().toString(), e.getValue()[0], e.getValue()[1])).toList();

        return new DashboardResponse(users.count(), lost, found, pending, approved, recovered,
                List.of(new NameCount("Lost", lost), new NameCount("Found", found)), monthly, claimStatuses,
                cat.entrySet().stream().map(e -> new NameCount(e.getKey(), e.getValue())).toList());
    }

    private static void bump(Map<YearMonth, long[]> months, Instant created, int idx) {
        if (created == null) return;
        long[] arr = months.get(YearMonth.from(created.atZone(ZoneId.systemDefault())));
        if (arr != null) arr[idx]++;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> users(String q, String role, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String query = q == null ? "" : q.trim();
        if (role == null || role.isBlank()) return PageResponse.of(users.search(query, p), UserResponse::from);
        RoleName rn;
        try { rn = RoleName.valueOf(role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase()); }
        catch (IllegalArgumentException e) { throw new ValidationFailedException(Map.of("role", "Unknown role")); }
        return PageResponse.of(users.searchByRole(query, rn, p), UserResponse::from);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled, User admin) {
        User u = users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (u.getId().equals(admin.getId())) throw new BusinessRuleException("You cannot disable your own account");
        u.setEnabled(enabled);
        users.save(u);
        activity.log(admin, enabled ? "USER_ENABLED" : "USER_DISABLED", "USER", id, u.getEmail(), !enabled);
        return UserResponse.from(u);
    }

    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> logs(boolean suspiciousOnly, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(suspiciousOnly ? logs.findBySuspicious(true, p) : logs.findAll(p), mapper::log);
    }

    // ---------- categories ----------
    public List<CategoryResponse> categories() {
        return categories.findAll(Sort.by("name")).stream().map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription())).toList();
    }

    private void checkCategory(CategoryRequest r) {
        if (r.name() == null || r.name().isBlank()) throw new ValidationFailedException(Map.of("name", "Category name is required"));
        if (r.name().trim().length() > 80) throw new ValidationFailedException(Map.of("name", "Category name must be at most 80 characters"));
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest r, User admin) {
        checkCategory(r);
        if (categories.existsByNameIgnoreCase(r.name().trim())) throw new DuplicateResourceException("Category already exists");
        Category c = categories.save(new Category(r.name().trim(), r.description()));
        activity.log(admin, "CATEGORY_CREATED", "CATEGORY", c.getId(), c.getName());
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest r, User admin) {
        checkCategory(r);
        Category c = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!c.getName().equalsIgnoreCase(r.name().trim()) && categories.existsByNameIgnoreCase(r.name().trim())) throw new DuplicateResourceException("Category already exists");
        c.setName(r.name().trim());
        c.setDescription(r.description());
        categories.save(c);
        activity.log(admin, "CATEGORY_UPDATED", "CATEGORY", id, c.getName());
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription());
    }

    @Transactional
    public void deleteCategory(Long id, User admin) {
        Category c = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (lostRepo.existsByCategory(c) || foundRepo.existsByCategory(c)) throw new BusinessRuleException("Category is in use by existing reports and cannot be deleted");
        categories.delete(c);
        activity.log(admin, "CATEGORY_DELETED", "CATEGORY", id, c.getName());
    }
}
