package com.lostfound.unit;

import com.lostfound.domain.*;
import com.lostfound.domain.Enums.*;

import java.time.LocalDate;

public final class TestData {
    private TestData() {}

    public static User user(long id, RoleName... roleNames) {
        User u = new User();
        u.setId(id);
        u.setFullName("User " + id);
        u.setEmail("user" + id + "@campus.edu");
        u.setPasswordHash("hash");
        u.setStudentId("STU" + (1000 + id));
        for (RoleName r : roleNames) u.getRoles().add(new Role(r));
        return u;
    }

    public static Category category(long id) {
        Category c = new Category("Cat" + id, null);
        c.setId(id);
        return c;
    }

    public static FoundItem found(long id, User reporter, FoundStatus status) {
        FoundItem f = new FoundItem();
        f.setId(id);
        f.setReporter(reporter);
        f.setName("Black Dell Laptop");
        f.setCategory(category(1));
        f.setDescription("A black laptop with stickers");
        f.setEventDate(LocalDate.now().minusDays(2));
        f.setLocation("Library");
        f.setHandoverLocation("Security Office");
        f.setStatus(status);
        return f;
    }

    public static LostItem lost(long id, User reporter, String name, long categoryId, String color, String brand, String location, LocalDate date) {
        LostItem l = new LostItem();
        l.setId(id);
        l.setReporter(reporter);
        l.setName(name);
        l.setCategory(category(categoryId));
        l.setColor(color);
        l.setBrand(brand);
        l.setLocation(location);
        l.setEventDate(date);
        return l;
    }

    public static Claim claim(long id, FoundItem item, User claimant, ClaimStatus status) {
        Claim c = new Claim();
        c.setId(id);
        c.setFoundItem(item);
        c.setClaimant(claimant);
        c.setStatus(status);
        c.setOwnershipExplanation("This is my laptop, I bought it last year");
        c.setDistinguishingCharacteristics("Sticker on lid");
        return c;
    }
}
