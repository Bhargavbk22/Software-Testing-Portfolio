package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.exception.BusinessRuleException;
import com.lostfound.exception.ResourceNotFoundException;
import com.lostfound.repo.FoundItemRepository;
import com.lostfound.repo.ItemMatchRepository;
import com.lostfound.repo.LostItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Rule-based matching. Scoring: category 25, similar name 25, colour 15, brand 15, nearby location 10, similar date 10.
 * The score is an indication only, never proof of ownership.
 */
@Service
public class MatchService {
    public static final int CATEGORY = 25, NAME = 25, COLOR = 15, BRAND = 15, LOCATION = 10, DATE = 10;

    private final ItemMatchRepository matches;
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final NotificationService notifications;
    private final StatusService status;
    private final ActivityService activity;
    private final int minScore;

    public MatchService(ItemMatchRepository matches, LostItemRepository lostRepo, FoundItemRepository foundRepo,
                        NotificationService notifications, StatusService status, ActivityService activity,
                        @Value("${app.matching.min-score:50}") int minScore) {
        this.matches = matches; this.lostRepo = lostRepo; this.foundRepo = foundRepo;
        this.notifications = notifications; this.status = status; this.activity = activity; this.minScore = minScore;
    }

    // ---------- pure scoring (unit-testable) ----------
    public static int score(LostItem l, FoundItem f) {
        int s = 0;
        if (l.getCategory() != null && f.getCategory() != null && l.getCategory().getId() != null
                && l.getCategory().getId().equals(f.getCategory().getId())) s += CATEGORY;
        if (similarName(l.getName(), f.getName())) s += NAME;
        if (sameText(l.getColor(), f.getColor())) s += COLOR;
        if (sameText(l.getBrand(), f.getBrand())) s += BRAND;
        if (nearbyLocation(l.getLocation(), f.getLocation())) s += LOCATION;
        if (l.getEventDate() != null && f.getEventDate() != null
                && Math.abs(ChronoUnit.DAYS.between(l.getEventDate(), f.getEventDate())) <= 7) s += DATE;
        return s;
    }

    static boolean sameText(String a, String b) {
        return a != null && b != null && !a.isBlank() && a.trim().equalsIgnoreCase(b.trim());
    }

    static boolean nearbyLocation(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        String x = a.trim().toLowerCase(Locale.ROOT), y = b.trim().toLowerCase(Locale.ROOT);
        return x.equals(y) || x.contains(y) || y.contains(x);
    }

    static boolean similarName(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return false;
        String x = a.trim().toLowerCase(Locale.ROOT), y = b.trim().toLowerCase(Locale.ROOT);
        if (x.equals(y) || x.contains(y) || y.contains(x)) return true;
        Set<String> ta = tokens(x), tb = tokens(y);
        if (ta.isEmpty() || tb.isEmpty()) return false;
        Set<String> inter = new HashSet<>(ta);
        inter.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) inter.size() / union.size() >= 0.5;
    }

    private static Set<String> tokens(String s) {
        Set<String> t = new HashSet<>();
        Arrays.stream(s.split("[^a-z0-9]+")).filter(w -> w.length() >= 3).forEach(t::add);
        return t;
    }

    // ---------- persistence ----------
    @Transactional
    public void runForLost(LostItem l) {
        List<FoundItem> candidates = foundRepo.findByStatusIn(List.of(FoundStatus.UNDER_REVIEW, FoundStatus.AVAILABLE, FoundStatus.CLAIM_PENDING));
        for (FoundItem f : candidates) createIfGood(l, f);
    }

    @Transactional
    public void runForFound(FoundItem f) {
        List<LostItem> candidates = lostRepo.findByStatusIn(List.of(LostStatus.REPORTED, LostStatus.UNDER_REVIEW));
        for (LostItem l : candidates) createIfGood(l, f);
    }

    private void createIfGood(LostItem l, FoundItem f) {
        if (matches.existsByLostItemAndFoundItem(l, f)) return;
        int s = score(l, f);
        if (s < minScore) return;
        ItemMatch m = new ItemMatch();
        m.setLostItem(l);
        m.setFoundItem(f);
        m.setScore(s);
        matches.save(m);
        notifications.notify(l.getReporter(), NotificationType.POTENTIAL_MATCH,
                "Possible match found for your lost item \"" + l.getName() + "\" (score " + s + "/100). This is an indication, not proof of ownership.", "LOST", l.getId());
        notifications.notifyRole(RoleName.ROLE_MODERATOR, NotificationType.POTENTIAL_MATCH,
                "New potential match: \"" + l.getName() + "\" ↔ \"" + f.getName() + "\" (score " + s + ")", "MATCH", m.getId());
    }

    public Page<ItemMatch> list(MatchStatus st, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "score"));
        return st == null ? matches.findAll(pr) : matches.findByStatus(st, pr);
    }

    @Transactional
    public ItemMatch confirm(Long id, User moderator) {
        ItemMatch m = matches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (m.getStatus() != MatchStatus.SUGGESTED) throw new BusinessRuleException("Only suggested matches can be confirmed");
        LostItem l = m.getLostItem();
        if (l.getStatus() == LostStatus.REPORTED) status.lost(l, LostStatus.UNDER_REVIEW, moderator, "Match review started");
        if (l.getStatus() != LostStatus.MATCHED) status.lost(l, LostStatus.MATCHED, moderator, "Match confirmed by moderator");
        m.setStatus(MatchStatus.CONFIRMED);
        m.setReviewedBy(moderator);
        matches.save(m);
        notifications.notify(l.getReporter(), NotificationType.POTENTIAL_MATCH,
                "A moderator confirmed a likely match for \"" + l.getName() + "\". You can now submit a claim on the found item.", "FOUND", m.getFoundItem().getId());
        activity.log(moderator, "MATCH_CONFIRMED", "MATCH", m.getId(), "score " + m.getScore());
        return m;
    }

    @Transactional
    public ItemMatch dismiss(Long id, User moderator) {
        ItemMatch m = matches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Match not found"));
        if (m.getStatus() != MatchStatus.SUGGESTED) throw new BusinessRuleException("Only suggested matches can be dismissed");
        m.setStatus(MatchStatus.DISMISSED);
        m.setReviewedBy(moderator);
        activity.log(moderator, "MATCH_DISMISSED", "MATCH", m.getId(), null);
        return matches.save(m);
    }
}
