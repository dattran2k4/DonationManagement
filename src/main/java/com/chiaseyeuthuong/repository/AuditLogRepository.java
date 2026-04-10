package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    long countByEntityTypeAndEntityId(EEntityType entityType, Long entityId);
}
