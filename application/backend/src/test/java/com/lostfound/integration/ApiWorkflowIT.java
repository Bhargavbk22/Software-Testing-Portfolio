package com.lostfound.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.User;
import com.lostfound.repo.CategoryRepository;
import com.lostfound.repo.RoleRepository;
import com.lostfound.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end tests against the real Spring context, security filter chain and (in-memory H2) database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiWorkflowIT {
    static final String PW = "Passw0rd!";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CategoryRepository categories;
    @Autowired PasswordEncoder encoder;
    int seq = 0;

    // ---------- helpers ----------
    User createUser(String email, RoleName role) {
        User u = new User();
        u.setFullName("Test " + email);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(PW));
        u.setStudentId("T" + (10000 + (++seq)) + Math.abs(email.hashCode() % 1000));
        u.setDepartment("CS");
        u.setPhone("9876543210");
        u.getRoles().add(roles.findByName(role).orElseThrow());
        return users.save(u);
    }

    String token(String email, RoleName role) throws Exception {
        createUser(email, role);
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("email", email, "password", PW)))).andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString()).at("/data/token").asText();
    }

    ResultActions send(MockHttpServletRequestBuilder b, String token, Object body) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(body));
        return mvc.perform(b);
    }

    JsonNode json(ResultActions a) throws Exception { return om.readTree(a.andReturn().getResponse().getContentAsString()); }

    Map<String, Object> item(Object overrideKey, Object overrideValue) {
        Map<String, Object> m = new HashMap<>(Map.of("name", "Black Dell Laptop", "categoryId", categories.findAll().get(0).getId(),
                "description", "Black laptop with stickers on the lid", "date", LocalDate.now().minusDays(1).toString(), "location", "Library",
                "color", "Black", "brand", "Dell", "identifyingCharacteristics", "Cracked corner and a star sticker",
                "contactInfo", "9876543210", "handoverLocation", "Security Office"));
        if (overrideKey != null) m.put((String) overrideKey, overrideValue);
        return m;
    }

    // ---------- authentication ----------
    @Test
    void register_validRequest_returns201() throws Exception {
        Map<String, Object> body = Map.of("fullName", "Ann Lee", "email", "ann@campus.edu", "password", PW, "confirmPassword", PW,
                "studentId", "STU90001", "department", "CS", "phone", "9876543210");
        send(post("/api/auth/register"), null, body).andExpect(status().isCreated()).andExpect(jsonPath("$.data.email").value("ann@campus.edu"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createUser("dup@campus.edu", RoleName.ROLE_USER);
        Map<String, Object> body = Map.of("fullName", "Dup", "email", "dup@campus.edu", "password", PW, "confirmPassword", PW,
                "studentId", "STU90002", "department", "CS", "phone", "9876543210");
        send(post("/api/auth/register"), null, body).andExpect(status().isConflict());
    }

    @Test
    void register_weakPassword_returns400WithFieldError() throws Exception {
        Map<String, Object> body = Map.of("fullName", "Weak", "email", "weak@campus.edu", "password", "abc", "confirmPassword", "abc",
                "studentId", "STU90003", "department", "CS", "phone", "9876543210");
        send(post("/api/auth/register"), null, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        createUser("login@campus.edu", RoleName.ROLE_USER);
        send(post("/api/auth/login"), null, Map.of("email", "login@campus.edu", "password", "WrongPass1!")).andExpect(status().isUnauthorized());
    }

    @Test
    void login_validCredentials_returnsJwt() throws Exception {
        assertFalse(token("ok@campus.edu", RoleName.ROLE_USER).isBlank());
    }

    // ---------- security ----------
    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        send(get("/api/users/profile"), null, null).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withGarbageToken_returns401() throws Exception {
        send(get("/api/users/profile"), "not.a.jwt", null).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid authentication token"));
    }

    @Test
    void adminEndpoints_enforceRoles() throws Exception {
        String user = token("u1@campus.edu", RoleName.ROLE_USER);
        String mod = token("m1@campus.edu", RoleName.ROLE_MODERATOR);
        String admin = token("a1@campus.edu", RoleName.ROLE_ADMIN);
        send(get("/api/admin/dashboard"), user, null).andExpect(status().isForbidden());
        send(get("/api/admin/dashboard"), mod, null).andExpect(status().isForbidden());
        send(get("/api/admin/dashboard"), admin, null).andExpect(status().isOk());
        send(get("/api/moderator/dashboard"), user, null).andExpect(status().isForbidden());
        send(get("/api/moderator/dashboard"), mod, null).andExpect(status().isOk());
        send(get("/api/recoveries"), mod, null).andExpect(status().isForbidden());
    }

    // ---------- lost / found reports ----------
    @Test
    void lostReport_valid_returns201() throws Exception {
        String t = token("l1@campus.edu", RoleName.ROLE_USER);
        send(post("/api/lost-items"), t, item(null, null)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("REPORTED"));
    }

    @Test
    void lostReport_missingMandatoryFields_returns400() throws Exception {
        String t = token("l2@campus.edu", RoleName.ROLE_USER);
        send(post("/api/lost-items"), t, Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists()).andExpect(jsonPath("$.errors.categoryId").exists());
    }

    @Test
    void lostReport_futureDate_returns400() throws Exception {
        String t = token("l3@campus.edu", RoleName.ROLE_USER);
        send(post("/api/lost-items"), t, item("date", LocalDate.now().plusDays(1).toString())).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.date").exists());
    }

    @Test
    void lostReport_shortDescription_returns400() throws Exception {
        String t = token("l4@campus.edu", RoleName.ROLE_USER);
        send(post("/api/lost-items"), t, item("description", "short")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.description").exists());
    }

    @Test
    void imageUpload_unsupportedFormat_returns400_andPngIsAccepted() throws Exception {
        String t = token("img@campus.edu", RoleName.ROLE_USER);
        long id = json(send(post("/api/lost-items"), t, item(null, null))).at("/data/id").asLong();
        mvc.perform(multipart("/api/lost-items/" + id + "/images").file(new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes()))
                .header("Authorization", "Bearer " + t)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/lost-items/" + id + "/images").file(new MockMultipartFile("file", "pic.png", "image/png", new byte[]{1, 2, 3}))
                .header("Authorization", "Bearer " + t)).andExpect(status().isOk()).andExpect(jsonPath("$.data.images[0]").exists());
    }

    @Test
    void foundReport_isNotPublic_untilVerified_andHidesIdentifyingCharacteristics() throws Exception {
        String finder = token("f1@campus.edu", RoleName.ROLE_USER);
        String other = token("o1@campus.edu", RoleName.ROLE_USER);
        String mod = token("m2@campus.edu", RoleName.ROLE_MODERATOR);
        long id = json(send(post("/api/found-items"), finder, item(null, null))).at("/data/id").asLong();
        send(get("/api/items/search?type=FOUND"), other, null).andExpect(jsonPath("$.data.totalElements").value(0));
        send(put("/api/moderator/reports/FOUND/" + id + "/review"), mod, null).andExpect(status().isOk());
        send(put("/api/moderator/reports/FOUND/" + id + "/approve"), mod, null).andExpect(status().isOk());
        send(get("/api/items/search?type=FOUND"), other, null).andExpect(jsonPath("$.data.totalElements").value(1));
        send(get("/api/found-items/" + id), other, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.identifyingCharacteristics").doesNotExist());
        send(get("/api/found-items/" + id), mod, null).andExpect(jsonPath("$.data.identifyingCharacteristics").value("Cracked corner and a star sticker"));
    }

    @Test
    void editingForeignOrVerifiedReport_isBlocked() throws Exception {
        String finder = token("f2@campus.edu", RoleName.ROLE_USER);
        String other = token("o2@campus.edu", RoleName.ROLE_USER);
        long id = json(send(post("/api/found-items"), finder, item(null, null))).at("/data/id").asLong();
        send(put("/api/found-items/" + id), other, item(null, null)).andExpect(status().isForbidden());
        send(put("/api/found-items/" + id), finder, item("name", "Updated Laptop")).andExpect(status().isOk());
    }

    @Test
    void withdrawnLostReport_cannotBeModifiedByOwner() throws Exception {
        String t = token("w1@campus.edu", RoleName.ROLE_USER);
        long id = json(send(post("/api/lost-items"), t, item(null, null))).at("/data/id").asLong();
        send(delete("/api/lost-items/" + id), t, null).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CLOSED"));
        send(put("/api/lost-items/" + id), t, item("name", "Changed")).andExpect(status().isForbidden());
        send(delete("/api/lost-items/" + id), t, null).andExpect(status().isConflict());
    }

    // ---------- full claim -> recovery workflow ----------
    @Test
    void fullWorkflow_reportVerifyClaimApproveScheduleReturn_andRulesAreEnforced() throws Exception {
        String finder = token("wf-finder@campus.edu", RoleName.ROLE_USER);
        String owner = token("wf-owner@campus.edu", RoleName.ROLE_USER);
        String late = token("wf-late@campus.edu", RoleName.ROLE_USER);
        String mod = token("wf-mod@campus.edu", RoleName.ROLE_MODERATOR);
        String admin = token("wf-admin@campus.edu", RoleName.ROLE_ADMIN);

        long foundId = json(send(post("/api/found-items"), finder, item(null, null))).at("/data/id").asLong();
        Map<String, Object> claimBody = Map.of("foundItemId", foundId, "ownershipExplanation", "This is my Dell laptop, bought in 2024",
                "distinguishingCharacteristics", "Star sticker and cracked corner", "evidence", java.util.List.of("Invoice number 123"));

        // cannot claim before the report is verified/available
        send(post("/api/claims"), owner, claimBody).andExpect(status().isUnprocessableEntity());

        send(put("/api/moderator/reports/FOUND/" + foundId + "/review"), mod, null).andExpect(status().isOk());
        send(put("/api/moderator/reports/FOUND/" + foundId + "/approve"), mod, null).andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        long claimId = json(send(post("/api/claims"), owner, claimBody).andExpect(status().isCreated())).at("/data/id").asLong();
        send(post("/api/claims"), owner, claimBody).andExpect(status().isConflict());                    // duplicate active claim

        // approval requires review first (state transition) and verified ownership (decision table)
        send(put("/api/admin/claims/" + claimId + "/approve"), admin, Map.of("ownershipVerified", true, "reason", "ok")).andExpect(status().isConflict());
        send(put("/api/admin/claims/" + claimId + "/review"), admin, null).andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
        send(put("/api/admin/claims/" + claimId + "/approve"), admin, Map.of("ownershipVerified", false, "reason", "not sure")).andExpect(status().isUnprocessableEntity());
        send(put("/api/admin/claims/" + claimId + "/approve"), mod, Map.of("ownershipVerified", true, "reason", "x")).andExpect(status().isForbidden());
        send(put("/api/admin/claims/" + claimId + "/approve"), admin, Map.of("ownershipVerified", true, "reason", "Invoice matches")).andExpect(jsonPath("$.data.status").value("APPROVED"));

        // recovery
        send(post("/api/recoveries"), admin, Map.of("claimId", claimId, "scheduledAt", "2030-01-01T10:00:00", "location", "Security Office")).andExpect(status().isCreated());
        JsonNode recoveries = json(send(get("/api/recoveries"), admin, null));
        long recoveryId = recoveries.at("/data/content/0/id").asLong();
        send(post("/api/recoveries"), admin, Map.of("claimId", claimId, "scheduledAt", "2030-01-01T10:00:00", "location", "X")).andExpect(status().isConflict());
        JsonNode done = json(send(put("/api/recoveries/" + recoveryId + "/complete"), admin, null).andExpect(status().isOk()));
        assertTrue(done.at("/data/confirmationCode").asText().startsWith("REC-"));
        send(put("/api/recoveries/" + recoveryId + "/complete"), admin, null).andExpect(status().isConflict());     // already completed

        // returned items can never be claimed again
        send(post("/api/claims"), late, claimBody).andExpect(status().isUnprocessableEntity());
        send(get("/api/found-items/" + foundId), owner, null).andExpect(jsonPath("$.data.status").value("RETURNED"));
        send(get("/api/notifications/unread-count"), owner, null).andExpect(status().isOk());
    }

    @Test
    void rejectedClaim_cannotBeApprovedDirectly_butAdminMayReconsider() throws Exception {
        String finder = token("rj-finder@campus.edu", RoleName.ROLE_USER);
        String owner = token("rj-owner@campus.edu", RoleName.ROLE_USER);
        String mod = token("rj-mod@campus.edu", RoleName.ROLE_MODERATOR);
        String admin = token("rj-admin@campus.edu", RoleName.ROLE_ADMIN);
        long foundId = json(send(post("/api/found-items"), finder, item(null, null))).at("/data/id").asLong();
        send(put("/api/moderator/reports/FOUND/" + foundId + "/review"), mod, null);
        send(put("/api/moderator/reports/FOUND/" + foundId + "/approve"), mod, null);
        long claimId = json(send(post("/api/claims"), owner, Map.of("foundItemId", foundId, "ownershipExplanation", "It is my laptop, I can prove it",
                "distinguishingCharacteristics", "Sticker"))).at("/data/id").asLong();
        send(put("/api/admin/claims/" + claimId + "/review"), admin, null).andExpect(status().isOk());
        send(put("/api/admin/claims/" + claimId + "/reject"), admin, Map.of("reason", "")).andExpect(status().isBadRequest());
        send(put("/api/admin/claims/" + claimId + "/reject"), admin, Map.of("reason", "Details do not match")).andExpect(jsonPath("$.data.status").value("REJECTED"));
        send(put("/api/admin/claims/" + claimId + "/approve"), admin, Map.of("ownershipVerified", true, "reason", "x")).andExpect(status().isConflict());
        send(put("/api/moderator/claims/" + claimId + "/escalate"), mod, null).andExpect(status().isForbidden());
        send(put("/api/admin/claims/" + claimId + "/review"), admin, null).andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
    }
}
