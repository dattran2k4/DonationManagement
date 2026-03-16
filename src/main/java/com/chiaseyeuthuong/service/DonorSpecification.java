package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.model.Donor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class DonorSpecification {
    private DonorSpecification() {
    }

    public static Specification<Donor> filterDonor(String search, EDonorType type) {
        return (Root<Donor> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }

            if (StringUtils.hasLength(search)) {
                String pattern = String.format("%%%s%%", search.trim().toLowerCase());
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }

            return predicate;
        };
    }
}
