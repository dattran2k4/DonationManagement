package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.Transaction;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    boolean existsByTransactionCode(@NonNull String reference);

    Optional<Transaction> findByTransactionCode(String transactionCode);

    Optional<Transaction> findByDonationId(Long donationId);
}
