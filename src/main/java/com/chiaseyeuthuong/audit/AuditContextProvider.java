package com.chiaseyeuthuong.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditContextProvider {

    public AuditContext getCurrentContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String username = "anonymous";
        String role = "ANONYMOUS";
        if (authentication != null && authentication.isAuthenticated()) {
            if (StringUtils.hasText(authentication.getName())) {
                username = authentication.getName();
            }
            role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse("UNKNOWN");
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new AuditContext(username, role, null, null);
        }

        HttpServletRequest request = attributes.getRequest();
        String ipAddress = resolveIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        return new AuditContext(username, role, ipAddress, userAgent);
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
