package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.audit.AuditContext;
import com.chiaseyeuthuong.audit.AuditContextProvider;
import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.response.AuditChangeItemResponse;
import com.chiaseyeuthuong.dto.response.AuditLogResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.AuditLog;
import com.chiaseyeuthuong.repository.AuditLogRepository;
import com.chiaseyeuthuong.service.AuditLogService;
import com.chiaseyeuthuong.service.AuditLogSpecification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "AUDIT-LOG-SERVICE")
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditContextProvider auditContextProvider;
    private final ObjectMapper objectMapper;

    @Override
    public void logCreate(EEntityType entityType, Long entityId, String summary, Map<String, Object> afterValues) {
        List<AuditChangeItemResponse> changes = afterValues.entrySet()
                .stream()
                .map(entry -> AuditChangeItemResponse.builder()
                        .field(entry.getKey())
                        .oldValue(null)
                        .newValue(stringify(entry.getValue()))
                        .build())
                .toList();
        saveAuditLog(EAuditAction.CREATE, entityType, entityId, summary, changes);
    }

    @Override
    public void logUpdate(EEntityType entityType, Long entityId, String summary, Map<String, Object> beforeValues, Map<String, Object> afterValues) {
        List<AuditChangeItemResponse> changes = diff(beforeValues, afterValues);
        if (changes.isEmpty()) {
            return;
        }
        saveAuditLog(EAuditAction.UPDATE, entityType, entityId, summary, changes);
    }

    @Override
    public void logStatusChange(EEntityType entityType, Long entityId, String summary, String oldStatus, String newStatus) {
        if (Objects.equals(oldStatus, newStatus)) {
            return;
        }
        List<AuditChangeItemResponse> changes = List.of(AuditChangeItemResponse.builder()
                .field("status")
                .oldValue(oldStatus)
                .newValue(newStatus)
                .build());
        saveAuditLog(EAuditAction.STATUS_CHANGE, entityType, entityId, summary, changes);
    }

    @Override
    public PageResponse<AuditLogResponse> getAuditLogs(int page, int size, EEntityType entityType, Long entityId, EAuditAction action,
                                                       String actorUsername, String keyword, LocalDateTime fromDate, LocalDateTime toDate) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<AuditLog> specification = AuditLogSpecification.filter(entityType, entityId, action, actorUsername, keyword, fromDate, toDate);
        Page<AuditLog> auditLogs = auditLogRepository.findAll(specification, pageable);

        List<AuditLogResponse> data = auditLogs.getContent().stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<AuditLogResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(auditLogs.getTotalElements())
                .totalPages(auditLogs.getTotalPages())
                .data(data)
                .build();
    }

    private void saveAuditLog(EAuditAction action, EEntityType entityType, Long entityId, String summary, List<AuditChangeItemResponse> changes) {
        try {
            AuditContext context = auditContextProvider.getCurrentContext();

            AuditLog auditLog = new AuditLog();
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setSummary(summary);
            auditLog.setActorUsername(context.username());
            auditLog.setActorRole(context.role());
            auditLog.setIpAddress(context.ipAddress());
            auditLog.setUserAgent(context.userAgent());
            auditLog.setChangesJson(objectMapper.writeValueAsString(changes));

            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Cannot save audit log: {}", ex.getMessage(), ex);
        }
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .actorUsername(auditLog.getActorUsername())
                .actorRole(auditLog.getActorRole())
                .summary(auditLog.getSummary())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .changes(parseChanges(auditLog.getChangesJson()))
                .build();
    }

    private List<AuditChangeItemResponse> parseChanges(String changesJson) {
        if (changesJson == null || changesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(changesJson, new TypeReference<>() {
            });
        } catch (Exception ex) {
            log.error("Cannot parse audit changes json: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    private List<AuditChangeItemResponse> diff(Map<String, Object> beforeValues, Map<String, Object> afterValues) {
        List<AuditChangeItemResponse> changes = new ArrayList<>();

        for (Map.Entry<String, Object> entry : afterValues.entrySet()) {
            String field = entry.getKey();
            Object afterValue = entry.getValue();
            Object beforeValue = beforeValues.get(field);

            if (Objects.equals(normalize(beforeValue), normalize(afterValue))) {
                continue;
            }

            changes.add(AuditChangeItemResponse.builder()
                    .field(field)
                    .oldValue(stringify(beforeValue))
                    .newValue(stringify(afterValue))
                    .build());
        }

        return changes;
    }

    private Object normalize(Object value) {
        if (value == null) return null;
        if (value instanceof String string) {
            return string.trim();
        }
        return value;
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
