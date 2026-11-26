package com.lostfound.unit;

import com.lostfound.domain.FoundItem;
import com.lostfound.domain.LostItem;
import com.lostfound.domain.User;
import com.lostfound.domain.Enums.FoundStatus;
import com.lostfound.service.MatchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchServiceTest {
    private final User u = TestData.user(1);
    private final LocalDate d = LocalDate.of(2026, 6, 10);

    private FoundItem found(String name, long cat, String color, String brand, String loc, LocalDate date) {
        FoundItem f = TestData.found(1, u, FoundStatus.AVAILABLE);
        f.setName(name); f.setCategory(TestData.category(cat)); f.setColor(color); f.setBrand(brand); f.setLocation(loc); f.setEventDate(date);
        return f;
    }

    @Test
    void identicalItems_scoreOneHundred() {
        LostItem l = TestData.lost(1, u, "Black Dell Laptop", 1, "Black", "Dell", "Library", d);
        assertEquals(100, MatchService.score(l, found("Black Dell Laptop", 1, "black", "dell", "library", d)));
    }

    @Test
    void nothingInCommon_scoresZero() {
        LostItem l = TestData.lost(1, u, "Blue Umbrella", 1, "Blue", "Generic", "Gym", d);
        assertEquals(0, MatchService.score(l, found("Silver Laptop", 2, "Silver", "Dell", "Library", d.plusDays(30))));
    }

    @Test
    void categoryOnly_scoresTwentyFive() {
        LostItem l = TestData.lost(1, u, "Blue Umbrella", 3, "Blue", null, "Gym", d);
        assertEquals(MatchService.CATEGORY, MatchService.score(l, found("Silver Laptop", 3, "Silver", "Dell", "Library", d.plusDays(30))));
    }

    @Test
    void dateBoundary_sevenDaysCounts_eightDoesNot() {
        LostItem l = TestData.lost(1, u, "AAA", 1, null, null, "X", d);
        assertEquals(MatchService.CATEGORY + MatchService.DATE, MatchService.score(l, found("ZZZ", 1, null, null, "Y", d.plusDays(7))));
        assertEquals(MatchService.CATEGORY, MatchService.score(l, found("ZZZ", 1, null, null, "Y", d.plusDays(8))));
    }

    @Test
    void similarNames_withReorderedWords_areMatched() {
        LostItem l = TestData.lost(1, u, "Black Dell Laptop", 9, null, null, "X", d.minusDays(60));
        assertTrue(MatchService.score(l, found("Dell laptop black", 1, null, null, "Y", d)) >= MatchService.NAME);
    }
}
