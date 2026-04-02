package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.response.AuditLogResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;

import java.time.LocalDateTime;
import java.util.Map;

public interface AuditLogService {

    void logCreate(EEntityType entityType, Long entityId, String summary, Map<String, Object> afterValues);

    void logUpdate(EEntityType entityType, Long entityId, String summary, Map<String, Object> beforeValues, Map<String, Object> afterValues);

    void logStatusChange(EEntityType entityType, Long entityId, String summary, String oldStatus, String newStatus);

    PageResponse<AuditLogResponse> getAuditLogs(int page, int size, EEntityType entityType, Long entityId, EAuditAction action,
                                                String actorUsername, String keyword, LocalDateTime fromDate, LocalDateTime toDate);
}
