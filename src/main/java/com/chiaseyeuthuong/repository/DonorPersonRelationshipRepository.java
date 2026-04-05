package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.DonorPersonRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorPersonRelationshipRepository extends JpaRepository<DonorPersonRelationship, Long> {

    List<DonorPersonRelationship> findByDonorIdAndIsActiveTrueOrderByUpdatedAtDescIdDesc(Long donorId);

    Optional<DonorPersonRelationship> findByIdAndDonorIdAndIsActiveTrue(Long id, Long donorId);

    Optional<DonorPersonRelationship> findFirstByDonorIdAndRelatedDonorIdAndRelationshipTypeIdAndIsActiveTrue(
            Long donorId,
            Long relatedDonorId,
            Long relationshipTypeId
    );
}
