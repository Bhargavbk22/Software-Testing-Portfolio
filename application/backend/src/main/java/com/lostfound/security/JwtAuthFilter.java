package com.lostfound.security;

import com.lostfound.domain.User;
import com.lostfound.repo.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;

    public JwtAuthFilter(JwtService jwt, UserRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                String email = jwt.extractEmail(header.substring(7));
                User user = users.findByEmailIgnoreCase(email).orElse(null);
                if (user != null && user.isEnabled()) {
                    var authorities = user.getRoles().stream().map(r -> new SimpleGrantedAuthority(r.getName().name())).toList();
                    SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
                } else {
                    req.setAttribute("auth_error", "Account not found or disabled");
                }
            } catch (ExpiredJwtException e) {
                req.setAttribute("auth_error", "Authentication token has expired. Please log in again.");
            } catch (JwtException | IllegalArgumentException e) {
                req.setAttribute("auth_error", "Invalid authentication token");
            }
        }
        chain.doFilter(req, res);
    }
}
