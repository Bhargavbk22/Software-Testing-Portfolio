package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.dto.Dto.*;
import com.lostfound.dto.PageResponse;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.exception.ValidationFailedException;
import com.lostfound.repo.CategoryRepository;
import com.lostfound.repo.FoundItemRepository;
import com.lostfound.repo.LostItemRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ItemService {
    private static final Set<LostStatus> LOST_TERMINAL = EnumSet.of(LostStatus.CLOSED, LostStatus.RESOLVED, LostStatus.REJECTED);
    private static final Set<FoundStatus> FOUND_TERMINAL = EnumSet.of(FoundStatus.RETURNED, FoundStatus.REJECTED);
    private static final List<LostStatus> LOST_PUBLIC = List.of(LostStatus.REPORTED, LostStatus.UNDER_REVIEW, LostStatus.MATCHED);
    private static final List<FoundStatus> FOUND_PUBLIC = List.of(FoundStatus.AVAILABLE, FoundStatus.CLAIM_PENDING);

    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final CategoryRepository categories;
    private final ValidationService validation;
    private final StatusService status;
    private final ActivityService activity;
    private final NotificationService notifications;
    private final MatchService matching;
    private final FileStorageService storage;
    private final Mapper mapper;

    private void apply(BaseItem i, ItemRequest r) {
        i.setCategory(categories.findById(r.categoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found")));
        i.setName(r.name().trim());
        i.setDescription(r.description().trim());
        i.setEventDate(r.date());
        i.setLocation(r.location().trim());
        i.setColor(blankToNull(r.color()));
        i.setBrand(blankToNull(r.brand()));
        i.setIdentifyingCharacteristics(blankToNull(r.identifyingCharacteristics()));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    // ================= LOST =================
    @Transactional
    public ItemResponse createLost(ItemRequest r, User user) {
        validation.validateItem(r, false);
        LostItem i = new LostItem();
        apply(i, r);
        i.setContactInfo(r.contactInfo().trim());
        i.setReporter(user);
        i.setStatus(LostStatus.REPORTED);
        lostRepo.save(i);
        status.initial("LOST", i.getId(), "REPORTED", user);
        notifications.notify(user, NotificationType.LOST_REPORT_SUBMITTED, "Your lost report \"" + i.getName() + "\" was submitted.", "LOST", i.getId());
        activity.log(user, "LOST_REPORT_CREATED", "LOST", i.getId(), i.getName());
        matching.runForLost(i);
        return mapper.item(i, user, true);
    }

    public LostItem loadLost(Long id) { return lostRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lost report not found")); }
    public FoundItem loadFound(Long id) { return foundRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Found report not found")); }

    @Transactional
    public ItemResponse updateLost(Long id, ItemRequest r, User user) {
        LostItem i = loadLost(id);
        boolean owner = i.getReporter().getId().equals(user.getId());
        if (!owner && !user.admin()) throw new AccessDeniedException("You can only edit your own reports");
        if (LOST_TERMINAL.contains(i.getStatus()) && !user.admin()) throw new AccessDeniedException("A closed report cannot be modified");
        if (!user.admin() && i.getStatus() == LostStatus.MATCHED) throw new BusinessRuleException("A matched report can no longer be edited");
        validation.validateItem(r, false);
        apply(i, r);
        i.setContactInfo(r.contactInfo().trim());
        lostRepo.save(i);
        activity.log(user, "LOST_REPORT_UPDATED", "LOST", i.getId(), i.getName());
        return mapper.item(i, user, true);
    }

    /** "Delete" is a withdrawal: the report is closed, never physically removed, so history stays intact. */
    @Transactional
    public ItemResponse withdrawLost(Long id, User user) {
        LostItem i = loadLost(id);
        if (!i.getReporter().getId().equals(user.getId()) && !user.admin()) throw new AccessDeniedException("You can only withdraw your own reports");
        status.lost(i, LostStatus.CLOSED, user, "Withdrawn by reporter");
        activity.log(user, "LOST_REPORT_WITHDRAWN", "LOST", i.getId(), i.getName());
        return mapper.item(i, user, true);
    }

    @Transactional(readOnly = true)
    public ItemResponse getLost(Long id, User viewer) {
        LostItem i = loadLost(id);
        if (!viewer.staff() && !i.getReporter().getId().equals(viewer.getId()) && !LOST_PUBLIC.contains(i.getStatus()))
            throw new ResourceNotFoundException("Lost report not found");
        return mapper.item(i, viewer, true);
    }

    // ================= FOUND =================
    @Transactional
    public ItemResponse createFound(ItemRequest r, User user) {
        validation.validateItem(r, true);
        FoundItem i = new FoundItem();
        apply(i, r);
        i.setHandoverLocation(r.handoverLocation().trim());
        i.setReporter(user);
        i.setStatus(FoundStatus.REPORTED);
        foundRepo.save(i);
        status.initial("FOUND", i.getId(), "REPORTED", user);
        notifications.notify(user, NotificationType.FOUND_REPORT_SUBMITTED, "Your found report \"" + i.getName() + "\" was submitted for verification.", "FOUND", i.getId());
        notifications.notifyRole(RoleName.ROLE_MODERATOR, NotificationType.FOUND_REPORT_SUBMITTED, "New found report awaiting review: \"" + i.getName() + "\"", "FOUND", i.getId());
        activity.log(user, "FOUND_REPORT_CREATED", "FOUND", i.getId(), i.getName());
        matching.runForFound(i);
        return mapper.item(i, user, true);
    }

    @Transactional
    public ItemResponse updateFound(Long id, ItemRequest r, User user) {
        FoundItem i = loadFound(id);
        boolean owner = i.getReporter().getId().equals(user.getId());
        if (!owner && !user.admin()) throw new AccessDeniedException("You can only edit your own reports");
        if (FOUND_TERMINAL.contains(i.getStatus()) && !user.admin()) throw new AccessDeniedException("A closed report cannot be modified");
        if (!user.admin() && i.getStatus() != FoundStatus.REPORTED) throw new BusinessRuleException("Reports can only be edited before verification starts");
        if (i.getStatus() == FoundStatus.RETURNED) throw new BusinessRuleException("A returned item's report cannot be modified");
        validation.validateItem(r, true);
        apply(i, r);
        i.setHandoverLocation(r.handoverLocation().trim());
        foundRepo.save(i);
        activity.log(user, "FOUND_REPORT_UPDATED", "FOUND", i.getId(), i.getName());
        return mapper.item(i, user, true);
    }

    @Transactional(readOnly = true)
    public ItemResponse getFound(Long id, User viewer) {
        FoundItem i = loadFound(id);
        boolean visible = FOUND_PUBLIC.contains(i.getStatus()) || i.getStatus() == FoundStatus.RETURNED;
        if (!viewer.staff() && !i.getReporter().getId().equals(viewer.getId()) && !visible) throw new ResourceNotFoundException("Found report not found");
        return mapper.item(i, viewer, true);
    }

    // ================= IMAGES =================
    @Transactional
    public ItemResponse uploadImage(String type, Long id, MultipartFile file, User user) {
        BaseItem i = "LOST".equalsIgnoreCase(type) ? loadLost(id) : loadFound(id);
        boolean owner = i.getReporter().getId().equals(user.getId());
        if (!owner && !user.admin()) throw new AccessDeniedException("You can only upload images to your own reports");
        boolean closed = i instanceof LostItem l ? LOST_TERMINAL.contains(l.getStatus()) : FOUND_TERMINAL.contains(((FoundItem) i).getStatus());
        if (closed && !user.admin()) throw new AccessDeniedException("A closed report cannot be modified");
        int existing = Mapper.images(i).size();
        validation.validateImage(file, existing);
        String name = storage.store(file);
        i.setImagePaths(existing == 0 ? name : i.getImagePaths() + "," + name);
        if (i instanceof LostItem l) lostRepo.save(l); else foundRepo.save((FoundItem) i);
        return mapper.item(i, user, true);
    }

    // ================= SEARCH =================
    private static <T extends BaseItem> Specification<T> spec(String q, Long categoryId, String location, LocalDate from, LocalDate to,
                                                               Enum<?> statusFilter, List<?> visibleStatuses, Long reporterId) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (q != null && !q.isBlank()) p.add(cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase() + "%"));
            if (categoryId != null) p.add(cb.equal(root.get("category").get("id"), categoryId));
            if (location != null && !location.isBlank()) p.add(cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"));
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("eventDate"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("eventDate"), to));
            if (statusFilter != null) p.add(cb.equal(root.get("status"), statusFilter));
            if (visibleStatuses != null) p.add(root.get("status").in(visibleStatuses));
            if (reporterId != null) p.add(cb.equal(root.get("reporter").get("id"), reporterId));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Sort sort(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) return Sort.by(Sort.Direction.ASC, "createdAt");
        if ("date".equalsIgnoreCase(sort)) return Sort.by(Sort.Direction.DESC, "eventDate");
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> search(String type, String q, Long categoryId, String location, LocalDate from, LocalDate to,
                                             String statusName, boolean mine, User viewer, int page, int size, String sort) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), sort(sort));
        Long reporter = mine ? viewer.getId() : null;
        boolean restricted = !viewer.staff() && !mine;
        if ("LOST".equalsIgnoreCase(type)) {
            LostStatus st = parse(LostStatus.class, statusName);
            Page<LostItem> res = lostRepo.findAll(spec(q, categoryId, location, from, to, st, restricted ? LOST_PUBLIC : null, reporter), pr);
            return PageResponse.of(res, i -> mapper.item(i, viewer, false));
        }
        FoundStatus st = parse(FoundStatus.class, statusName);
        Page<FoundItem> res = foundRepo.findAll(spec(q, categoryId, location, from, to, st, restricted ? FOUND_PUBLIC : null, reporter), pr);
        return PageResponse.of(res, i -> mapper.item(i, viewer, false));
    }

    private static <E extends Enum<E>> E parse(Class<E> c, String name) {
        if (name == null || name.isBlank()) return null;
        try {
            return Enum.valueOf(c, name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(Map.of("status", "Unknown status: " + name));
        }
    }
}
