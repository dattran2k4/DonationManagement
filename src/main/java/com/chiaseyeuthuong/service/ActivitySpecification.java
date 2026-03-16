package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EActivityStatus;
import com.chiaseyeuthuong.model.Activity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class ActivitySpecification {
    private ActivitySpecification() {
    }

    public static Specification<Activity> filterActivity(String search, EActivityStatus status) {
        return (Root<Activity> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (StringUtils.hasLength(search)) {
                String pattern = String.format("%%%s%%", search.trim().toLowerCase());
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern)
                ));
            }

            return predicate;
        };
    }
}
