package com.lostfound.service;

import com.lostfound.domain.Category;
import com.lostfound.domain.Enums.RoleName;
import com.lostfound.domain.Role;
import com.lostfound.repo.CategoryRepository;
import com.lostfound.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/** Ensures reference data (roles + default categories) exists in every environment, including tests. */
@Component
@Order(1)
@RequiredArgsConstructor
public class BootstrapData implements ApplicationRunner {
    private final RoleRepository roles;
    private final CategoryRepository categories;

    @Override
    public void run(ApplicationArguments args) {
        for (RoleName r : RoleName.values()) if (roles.findByName(r).isEmpty()) roles.save(new Role(r));
        if (categories.count() == 0) {
            List.of("Electronics", "Documents and ID Cards", "Bags and Accessories", "Books and Stationery",
                    "Wallets and Money", "Keys", "Clothing", "Other").forEach(n -> categories.save(new Category(n, null)));
        }
    }
}
