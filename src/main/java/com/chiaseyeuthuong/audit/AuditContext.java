package com.chiaseyeuthuong.audit;

public record AuditContext(
        String username,
        String role,
        String ipAddress,
        String userAgent
) {
}
