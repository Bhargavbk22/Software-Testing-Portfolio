package com.lostfound.security;

import com.lostfound.domain.User;
import com.lostfound.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    private CurrentUser() {}

    public static User get() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof User u)) throw new UnauthorizedException("Authentication required");
        return u;
    }
}
