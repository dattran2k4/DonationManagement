package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Donor;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

public class DonationSpecification {
    private DonationSpecification() {
    }

    public static Specification<Donation> filterDonation(String search, EDonationStatus status, EDonationTarget target,
                                                         EDonationType type, EPaymentMethod paymentMethod,
                                                         BigDecimal minAmount, BigDecimal maxAmount) {

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

            if (paymentMethod != null) {
                predicate = cb.and(predicate, cb.equal(root.get("paymentMethod"), paymentMethod));
            }

            if (minAmount != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            if (maxAmount != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            if (StringUtils.hasLength(search)) {
                String normalizedSearch = search.trim().toLowerCase();
                String pattern = String.format("%%%s%%", normalizedSearch);

                Join<Donation, Donor> donorJoin = root.join("donor");
                Predicate searchPredicate = cb.like(cb.lower(donorJoin.get("fullName")), pattern);

                String normalizedIdText = normalizedSearch.replace("dnt-", "").replace("#", "").trim();
                if (normalizedIdText.matches("\\d+")) {
                    try {
                        Long donationId = Long.parseLong(normalizedIdText);
                        searchPredicate = cb.or(
                                searchPredicate,
                                cb.equal(root.get("id"), donationId)
                        );
                    } catch (NumberFormatException ignored) {
                    }
                }

                predicate = cb.and(predicate, searchPredicate);
            }
            return predicate;
        };
    }
}
