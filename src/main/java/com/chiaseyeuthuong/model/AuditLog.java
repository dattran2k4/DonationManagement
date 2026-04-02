package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 32)
    private EAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private EEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "actor_username", length = 255)
    private String actorUsername;

    @Column(name = "actor_role", length = 100)
    private String actorRole;

    @Column(name = "summary", length = 500)
    private String summary;

    @Lob
    @Column(name = "changes_json", columnDefinition = "TEXT")
    private String changesJson;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
