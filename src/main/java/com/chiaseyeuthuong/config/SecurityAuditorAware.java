package com.chiaseyeuthuong.config;

import com.chiaseyeuthuong.security.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    private static final String SYSTEM_AUDITOR = "Hệ thống";

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(SYSTEM_AUDITOR);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.of(normalizeAuditor(customUserDetails.getUsername()));
        }
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(normalizeAuditor(userDetails.getUsername()));
        }
        if (principal instanceof String username) {
            return Optional.of(normalizeAuditor(username));
        }
        return Optional.of(SYSTEM_AUDITOR);
    }

    private String normalizeAuditor(String username) {
        if (!StringUtils.hasText(username) || "anonymousUser".equalsIgnoreCase(username)) {
            return SYSTEM_AUDITOR;
        }
        return username.trim();
    }
}
