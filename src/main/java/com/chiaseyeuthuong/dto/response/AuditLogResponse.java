package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private EAuditAction action;
    private EEntityType entityType;
    private Long entityId;
    private String actorUsername;
    private String actorRole;
    private String summary;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private List<AuditChangeItemResponse> changes;
}
