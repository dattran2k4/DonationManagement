package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Donor;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class DonationSpecification {
    private DonationSpecification() {
    }

    public static Specification<Donation> filterDonation(String search, EDonationStatus status, EDonationTarget target, EDonationType type) {

        return (Root<Donation> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {

            Predicate predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (target != null) {
                predicate = cb.and(predicate, cb.equal(root.get("target"), target));
            }

            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("type"), type));
            }


            if (StringUtils.hasLength(search)) {
                String pattern = String.format("%%%s%%", search.trim().toLowerCase());

                Join<Donation, Donor> donorJoin = root.join("donor");
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("memoCode")), pattern),
                        cb.like(cb.lower(donorJoin.get("fullName")), pattern)));
            }
            return predicate;
        };
    }
}
