package com.lostfound.service;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;
import com.lostfound.repo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Demo data for LOCAL DEVELOPMENT ONLY (2 admins, 2 moderators, 10 students, 15 lost + 15 found reports, claims, recoveries,
 * notifications, logs). Controlled by app.seed-demo-data; set SEED_DEMO_DATA=false for any shared/production environment.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final CategoryRepository categories;
    private final LostItemRepository lostRepo;
    private final FoundItemRepository foundRepo;
    private final ItemMatchRepository matchRepo;
    private final ClaimRepository claimRepo;
    private final ClaimEvidenceRepository evidenceRepo;
    private final RecoveryRepository recoveryRepo;
    private final NotificationRepository notifRepo;
    private final ActivityService activity;
    private final PasswordEncoder encoder;
    @Value("${app.seed-password}")
    private String seedPassword;

    // name, category, color, brand, location
    private static final String[][] DATA = {
            {"Black Dell Laptop", "Electronics", "Black", "Dell", "Central Library"},
            {"Student ID Card", "Documents and ID Cards", "White", "", "Main Gate"},
            {"Blue Backpack", "Bags and Accessories", "Blue", "Wildcraft", "Cafeteria"},
            {"Physics Textbook", "Books and Stationery", "White", "Pearson", "Lecture Hall 3"},
            {"Brown Leather Wallet", "Wallets and Money", "Brown", "Fossil", "Sports Complex"},
            {"Car Key with Remote", "Keys", "Black", "Honda", "Parking Lot B"},
            {"Grey Hoodie", "Clothing", "Grey", "Nike", "Computer Lab"},
            {"Wireless Earbuds", "Electronics", "White", "Apple", "Library Reading Room"},
            {"Scientific Calculator", "Books and Stationery", "Black", "Casio", "Exam Hall"},
            {"Silver Water Bottle", "Other", "Silver", "Milton", "Gymnasium"},
            {"Prescription Glasses", "Bags and Accessories", "Black", "RayBan", "Auditorium"},
            {"Red Umbrella", "Other", "Red", "", "Bus Stop"},
            {"Samsung Phone", "Electronics", "Black", "Samsung", "Canteen"},
            {"Hostel Room Key", "Keys", "Silver", "", "Hostel Block A"},
            {"Yellow Notebook", "Books and Stationery", "Yellow", "Classmate", "Library Reading Room"}
    };
    private static final String[] DEPTS = {"Computer Science", "Mechanical", "Electronics", "Civil", "Biotechnology"};

    private Map<String, Category> cats = new HashMap<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        categories.findAll().forEach(c -> cats.put(c.getName(), c));
        Role rUser = roles.findByName(RoleName.ROLE_USER).orElseThrow();
        Role rMod = roles.findByName(RoleName.ROLE_MODERATOR).orElseThrow();
        Role rAdmin = roles.findByName(RoleName.ROLE_ADMIN).orElseThrow();
        String hash = encoder.encode(seedPassword);

        User admin1 = user("Aarav Admin", "admin1@campus.edu", "ADM0001", "Administration", hash, rAdmin);
        User admin2 = user("Meera Admin", "admin2@campus.edu", "ADM0002", "Administration", hash, rAdmin);
        User mod1 = user("Rohan Moderator", "mod1@campus.edu", "MOD0001", "Student Affairs", hash, rMod);
        user("Sneha Moderator", "mod2@campus.edu", "MOD0002", "Student Affairs", hash, rMod);
        List<User> st = new ArrayList<>();
        String[] names = {"Anika Rao", "Karan Mehta", "Priya Nair", "Vikram Singh", "Divya Iyer", "Arjun Patel", "Neha Gupta", "Rahul Verma", "Isha Reddy", "Sameer Khan"};
        for (int i = 0; i < 10; i++)
            st.add(user(names[i], "student" + (i + 1) + "@campus.edu", "STU" + (1001 + i), DEPTS[i % 5], hash, rUser));

        LostStatus[] lostSt = {LostStatus.RESOLVED, LostStatus.RESOLVED, LostStatus.MATCHED, LostStatus.MATCHED, LostStatus.UNDER_REVIEW,
                LostStatus.UNDER_REVIEW, LostStatus.UNDER_REVIEW, LostStatus.REPORTED, LostStatus.REPORTED, LostStatus.REPORTED,
                LostStatus.REPORTED, LostStatus.REPORTED, LostStatus.REPORTED, LostStatus.CLOSED, LostStatus.REJECTED};
        FoundStatus[] foundSt = {FoundStatus.RETURNED, FoundStatus.RETURNED, FoundStatus.CLAIM_PENDING, FoundStatus.CLAIM_PENDING, FoundStatus.CLAIM_PENDING,
                FoundStatus.AVAILABLE, FoundStatus.AVAILABLE, FoundStatus.AVAILABLE, FoundStatus.AVAILABLE, FoundStatus.AVAILABLE,
                FoundStatus.UNDER_REVIEW, FoundStatus.UNDER_REVIEW, FoundStatus.REPORTED, FoundStatus.REPORTED, FoundStatus.REJECTED};

        List<LostItem> losts = new ArrayList<>();
        List<FoundItem> founds = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            User lostReporter = st.get(2 + (i % 8));
            LostItem l = new LostItem();
            fill(l, DATA[i], lostReporter, 3 + i, "Lost near " + DATA[i][4] + ". Please contact me if found.");
            l.setStatus(lostSt[i]);
            l.setContactInfo(lostReporter.getPhone());
            l.setVerified(lostSt[i] == LostStatus.MATCHED || lostSt[i] == LostStatus.RESOLVED || lostSt[i] == LostStatus.UNDER_REVIEW);
            lostRepo.save(l);
            activity.recordStatus("LOST", l.getId(), null, "REPORTED", lostReporter, "Created");
            if (lostSt[i] != LostStatus.REPORTED) activity.recordStatus("LOST", l.getId(), "REPORTED", lostSt[i].name(), mod1, "Seeded");
            losts.add(l);

            String[] fd = i < 8 ? DATA[i] : DATA[(i + 5) % 15];
            User finder = st.get(i % 2);
            FoundItem f = new FoundItem();
            fill(f, fd, finder, 2 + i, "Found at " + fd[4] + " and kept safe.");
            f.setStatus(foundSt[i]);
            f.setHandoverLocation("Security Office, Main Block");
            f.setVerified(foundSt[i] != FoundStatus.REPORTED && foundSt[i] != FoundStatus.UNDER_REVIEW && foundSt[i] != FoundStatus.REJECTED);
            foundRepo.save(f);
            activity.recordStatus("FOUND", f.getId(), null, "REPORTED", finder, "Created");
            if (foundSt[i] != FoundStatus.REPORTED) activity.recordStatus("FOUND", f.getId(), "REPORTED", foundSt[i].name(), mod1, "Seeded");
            founds.add(f);

            if (i < 8) {
                ItemMatch m = new ItemMatch();
                m.setLostItem(l);
                m.setFoundItem(f);
                m.setScore(MatchService.score(l, f));
                m.setStatus(i < 4 ? MatchStatus.CONFIRMED : MatchStatus.SUGGESTED);
                if (i < 4) m.setReviewedBy(mod1);
                matchRepo.save(m);
            }
        }

        // claims in every status
        Claim c0 = claim(founds.get(0), st.get(2), ClaimStatus.RETURNED, true, admin1, "Ownership verified with purchase invoice");
        Claim c1 = claim(founds.get(1), st.get(3), ClaimStatus.RETURNED, true, admin1, "Matched ID photo and student record");
        claim(founds.get(2), st.get(4), ClaimStatus.UNDER_REVIEW, false, null, null);
        claim(founds.get(3), st.get(5), ClaimStatus.PENDING, false, null, null);
        Claim c4 = claim(founds.get(4), st.get(6), ClaimStatus.HANDOVER_SCHEDULED, true, admin2, "Verified contents of wallet");
        claim(founds.get(5), st.get(7), ClaimStatus.REJECTED, false, admin1, "Could not describe distinguishing marks");
        claim(founds.get(6), st.get(8), ClaimStatus.CANCELLED, false, null, null);

        recovery(c0, RecoveryStatus.COMPLETED, admin1, "REC-" + java.time.Year.now().getValue() + "-00001");
        recovery(c1, RecoveryStatus.COMPLETED, admin1, "REC-" + java.time.Year.now().getValue() + "-00002");
        recovery(c4, RecoveryStatus.SCHEDULED, admin2, null);

        for (int i = 0; i < 10; i++) {
            note(st.get(i), NotificationType.GENERAL, "Welcome to Digital Lost & Found. Report a lost item or search for found ones.", false);
            note(st.get(i), NotificationType.POTENTIAL_MATCH, "A possible match was found for one of your reports.", i % 2 == 0);
        }
        note(st.get(2), NotificationType.RECOVERY_COMPLETED, "Your item was returned. Confirmation code issued.", false);
        note(st.get(5), NotificationType.CLAIM_SUBMITTED, "Your claim was submitted and is pending review.", false);
        note(st.get(7), NotificationType.CLAIM_REJECTED, "Your claim was rejected: could not describe distinguishing marks.", false);

        activity.log(admin1, "SYSTEM_SEEDED", "SYSTEM", null, "Demo data loaded");
        activity.log(null, "LOGIN_FAILED", "USER", null, "Failed login for unknown@campus.edu", true);
        activity.log(admin1, "CLAIM_REJECTED", "CLAIM", null, "Could not describe distinguishing marks", true);
        activity.log(mod1, "MATCH_CONFIRMED", "MATCH", null, "score 100");
    }

    private User user(String name, String email, String sid, String dept, String hash, Role role) {
        User u = new User();
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setStudentId(sid);
        u.setDepartment(dept);
        u.setPhone("98765" + String.format("%05d", Math.abs((email).hashCode()) % 100000));
        u.getRoles().add(role);
        return users.save(u);
    }

    private void fill(BaseItem i, String[] d, User reporter, int daysAgo, String desc) {
        i.setReporter(reporter);
        i.setName(d[0]);
        i.setCategory(cats.get(d[1]));
        i.setColor(d[2]);
        i.setBrand(d[3].isEmpty() ? null : d[3]);
        i.setLocation(d[4]);
        i.setDescription(d[2] + " " + d[0].toLowerCase() + ". " + desc);
        i.setEventDate(LocalDate.now().minusDays(daysAgo));
        i.setIdentifyingCharacteristics("Small scratch near the edge and a sticker with initials");
    }

    private Claim claim(FoundItem f, User claimant, ClaimStatus status, boolean verified, User reviewer, String reason) {
        Claim c = new Claim();
        c.setFoundItem(f);
        c.setClaimant(claimant);
        c.setOwnershipExplanation("This " + f.getName().toLowerCase() + " belongs to me; I lost it a few days ago.");
        c.setDistinguishingCharacteristics("It has a small scratch near the edge and a sticker with my initials.");
        c.setStatus(status);
        c.setOwnershipVerified(verified);
        c.setReviewedBy(reviewer);
        c.setDecisionReason(reason);
        claimRepo.save(c);
        evidenceRepo.save(new ClaimEvidence(c, "Photo of the item taken before it was lost"));
        activity.recordStatus("CLAIM", c.getId(), null, "PENDING", claimant, "Created");
        if (status != ClaimStatus.PENDING) activity.recordStatus("CLAIM", c.getId(), "PENDING", status.name(), reviewer, "Seeded");
        return c;
    }

    private void recovery(Claim c, RecoveryStatus st, User admin, String code) {
        Recovery r = new Recovery();
        r.setClaim(c);
        r.setStatus(st);
        r.setHandledBy(admin);
        r.setLocation("Security Office, Main Block");
        r.setScheduledAt(LocalDateTime.now().plusDays(st == RecoveryStatus.SCHEDULED ? 2 : -1));
        if (st == RecoveryStatus.COMPLETED) { r.setHandoverDate(Instant.now()); r.setConfirmationCode(code); }
        recoveryRepo.save(r);
    }

    private void note(User u, NotificationType t, String msg, boolean read) {
        Notification n = new Notification();
        n.setUser(u);
        n.setType(t);
        n.setMessage(msg);
        n.setReadStatus(read);
        notifRepo.save(n);
    }
}
