package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long>, JpaSpecificationExecutor<Donor> {

    Boolean existsByPhone(String phone);

    Optional<Donor> findByPhone(String phone);

    Optional<Donor> findByEmailIgnoreCase(String email);


    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "WHERE d.activity.id = :activityId " +
            "AND d.status = 'CONFIRMED'")
    long countDonorByActivityId(Long activityId);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "LEFT JOIN d.activity a " +
            "LEFT JOIN a.event ae " +
            "LEFT JOIN d.event e " +
            "WHERE d.status = 'CONFIRMED' " +
            "AND (e.id = :eventId OR ae.id = :eventId)")
    long countDonorByEventId(Long eventId);

    @Query("""
            SELECT donor
            FROM Donor donor
            WHERE EXISTS (
                SELECT 1
                FROM Donation donation
                LEFT JOIN donation.activity activity
                LEFT JOIN activity.event activityEvent
                LEFT JOIN donation.event event
                WHERE donation.donor.id = donor.id
                  AND donation.status = 'CONFIRMED'
                  AND (event.id = :eventId OR activityEvent.id = :eventId)
            )
            """)
    Page<Donor> findDonorsByEventId(Long eventId, Pageable pageable);

    @Query("""
            SELECT donor
            FROM Donor donor
            WHERE EXISTS (
                SELECT 1
                FROM Donation donation
                WHERE donation.donor.id = donor.id
                  AND donation.status = 'CONFIRMED'
                  AND donation.activity.id = :activityId
            )
            """)
    Page<Donor> findDonorsByActivityId(Long activityId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "WHERE d.status = 'CONFIRMED'")
    long countDonor();

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from, LocalDateTime to);
}
