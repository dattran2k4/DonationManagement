package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.model.Event;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class EventSpecification {

    private EventSpecification() {
    }

    public static Specification<Event> filterEvent(String search, EEventStatus status, String... categoryIds) {

        return (Root<Event> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            Predicate predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (categoryIds != null) {
                predicate = cb.and(predicate, root.get("category").get("id").in(categoryIds));
            }

            if (StringUtils.hasLength(search)) {
                String pattern = String.format("%%%s%%", search.trim().toLowerCase());

                predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), pattern));
            }
            return predicate;
        };
    }
}
