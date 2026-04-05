package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.OrganizationRoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRoleTypeRepository extends JpaRepository<OrganizationRoleType, Long> {

    List<OrganizationRoleType> findByIsActiveTrueOrderBySortOrderAscNameAsc();

    Optional<OrganizationRoleType> findByCode(String code);
}
