package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.Donor;
import jakarta.validation.constraints.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

    Boolean existsByPhone(String phone);

    Optional<Donor> findByPhone(String phone);


    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "WHERE d.activity.id = :activityId " +
            "AND d.status = 'CONFIRMED'")
    long countDonorByActivityId(Long activityId);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "WHERE d.activity.event.id = :eventId " +
            "AND d.status = 'CONFIRMED'")
    long countDonorByEventId(Long eventId);

    @Query("SELECT COUNT(DISTINCT d.donor.id) FROM Donation d " +
            "WHERE d.status = 'CONFIRMED'")
    long countDonor();
}
