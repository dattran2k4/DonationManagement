package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long>, JpaSpecificationExecutor<Donation> {
    interface DonorWallAggregation {
        Long getDonorId();

        String getDisplayName();

        String getFullName();

        BigDecimal getTotalAmount();

        Long getDonationCount();
    }

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

    long countByDonorId(Long donorId);

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

    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId, Pageable pageable);

    List<Donation> findAllByStatusAndTargetAndEventIsNotNull(EDonationStatus status, EDonationTarget target);

    List<Donation> findAllByStatusAndTargetAndEventIdIn(EDonationStatus status, EDonationTarget target, Collection<Long> eventIds);

    @EntityGraph(attributePaths = {"event", "activity"})
    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    @EntityGraph(attributePaths = {"event", "activity"})
    Page<Donation> findByDonorEmailIgnoreCase(String donorEmail, Pageable pageable);

    @EntityGraph(attributePaths = {"donor", "event", "activity"})
    @Query("""
            SELECT donation
            FROM Donation donation
            LEFT JOIN donation.event event
            LEFT JOIN donation.activity activity
            LEFT JOIN activity.event activityEvent
            WHERE event.id = :eventId OR activityEvent.id = :eventId
            """)
    Page<Donation> findByEventScopeId(Long eventId, Pageable pageable);

    @Query("""
            SELECT COUNT(donation.id)
            FROM Donation donation
            LEFT JOIN donation.event event
            LEFT JOIN donation.activity activity
            LEFT JOIN activity.event activityEvent
            WHERE event.id = :eventId OR activityEvent.id = :eventId
            """)
    long countByEventScopeId(Long eventId);

    @EntityGraph(attributePaths = {"donor", "event", "activity"})
    Page<Donation> findByActivityId(Long activityId, Pageable pageable);

    long countByActivityId(Long activityId);

    @Query("""
            SELECT donor.id AS donorId,
                   MAX(donor.displayName) AS displayName,
                   MAX(donor.fullName) AS fullName,
                   COALESCE(SUM(donation.amount), 0) AS totalAmount,
                   COUNT(donation.id) AS donationCount
            FROM Donation donation
            JOIN donation.donor donor
            WHERE donation.status = 'CONFIRMED'
              AND COALESCE(donation.donatedAt, donation.createdAt) >= :fromDate
              AND COALESCE(donation.donatedAt, donation.createdAt) <= :toDate
            GROUP BY donor.id
            ORDER BY COALESCE(SUM(donation.amount), 0) DESC, COUNT(donation.id) DESC
            """)
    List<DonorWallAggregation> aggregateDonorWall(LocalDateTime fromDate, LocalDateTime toDate);
}
