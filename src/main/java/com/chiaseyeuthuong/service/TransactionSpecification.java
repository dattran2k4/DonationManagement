package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.model.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class TransactionSpecification {
    private TransactionSpecification() {
    }

    public static Specification<Transaction> filterTransaction(String search, EPaymentMethod method) {
        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (method != null) {
                predicate = cb.and(predicate, cb.equal(root.get("paymentMethod"), method));
            }

            if (StringUtils.hasLength(search)) {
                String pattern = String.format("%%%s%%", search.trim().toLowerCase());
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("transactionCode")), pattern),
                        cb.like(cb.lower(root.get("counterAccountName")), pattern)
                ));
            }

            return predicate;
        };
    }
}
