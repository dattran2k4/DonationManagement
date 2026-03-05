package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {
    boolean existsByMemoCode(String memo);

    Optional<Donation> findByMemoCode(String memoCode);

    Optional<Donation> findByOrderCode(Long orderCode);

    @Query("""
                SELECT COALESCE(SUM(d.amount), 0)
                FROM Donation d
                WHERE d.status = 'CONFIRMED'
            """)
    BigDecimal sumConfirmedDonationsAmount();

    Integer countByDonorIdAndStatus(Long donorId, EDonationStatus status);

    @Query("""
                SELECT COALESCE(SUM(d.amount),0)
                FROM Donation d
                WHERE d.donor.id = :donorId
                AND d.status = :status
            """)
    BigDecimal sumAmountByDonorIdAndStatus(
            Long donorId,
            EDonationStatus status
    );
}
