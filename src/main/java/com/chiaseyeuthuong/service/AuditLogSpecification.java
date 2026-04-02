package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EAuditAction;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    public static Specification<AuditLog> filter(EEntityType entityType, Long entityId, EAuditAction action, String actorUsername,
                                                 String keyword, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            var predicates = criteriaBuilder.conjunction();

            if (entityType != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("entityType"), entityType));
            }

            if (entityId != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("entityId"), entityId));
            }

            if (action != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.equal(root.get("action"), action));
            }

            if (StringUtils.hasText(actorUsername)) {
                predicates = criteriaBuilder.and(predicates,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("actorUsername")), "%" + actorUsername.trim().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(keyword)) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("summary")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("changesJson")), normalizedKeyword)
                ));
            }

            if (fromDate != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }

            if (toDate != null) {
                predicates = criteriaBuilder.and(predicates, criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return predicates;
        };
    }
}
