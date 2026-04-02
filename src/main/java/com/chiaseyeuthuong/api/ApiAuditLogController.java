package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit-logs")
public class ApiAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getAuditLogs(@RequestParam(required = false, defaultValue = "1") int page,
                                    @RequestParam(required = false, defaultValue = "20") int size,
                                    @RequestParam(required = false) EEntityType entityType,
                                    @RequestParam(required = false) Long entityId,
                                    @RequestParam(required = false) EAuditAction action,
                                    @RequestParam(required = false) String actorUsername,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách audit log thành công")
                .data(auditLogService.getAuditLogs(page, size, entityType, entityId, action, actorUsername, keyword, fromDate, toDate))
                .build();
    }
}
