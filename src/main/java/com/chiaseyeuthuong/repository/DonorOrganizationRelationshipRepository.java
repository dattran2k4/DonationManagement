package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.DonorOrganizationRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorOrganizationRelationshipRepository extends JpaRepository<DonorOrganizationRelationship, Long> {

    List<DonorOrganizationRelationship> findByDonorIdAndIsActiveTrueOrderByUpdatedAtDescIdDesc(Long donorId);

    Optional<DonorOrganizationRelationship> findByIdAndDonorIdAndIsActiveTrue(Long id, Long donorId);

    Optional<DonorOrganizationRelationship> findFirstByDonorIdAndOrganizationDonorIdAndRoleTypeIdAndIsActiveTrue(
            Long donorId,
            Long organizationDonorId,
            Long roleTypeId
    );
}
