package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EUserStatus;
import com.chiaseyeuthuong.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {
    private UserSpecification() {
    }

    public static Specification<User> filterUsers(String search, EUserStatus status) {
        return (Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (status != null) {
                if (status == EUserStatus.ACTIVE) {
                    predicate = cb.and(predicate, cb.or(
                            cb.equal(root.get("status"), EUserStatus.ACTIVE),
                            cb.isNull(root.get("status"))
                    ));
                } else {
                    predicate = cb.and(predicate, cb.equal(root.get("status"), status));
                }
            }

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern)
                ));
            }

            return predicate;
        };
    }
}
