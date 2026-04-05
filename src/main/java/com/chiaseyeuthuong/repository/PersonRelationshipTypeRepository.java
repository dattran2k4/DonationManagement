package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.PersonRelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRelationshipTypeRepository extends JpaRepository<PersonRelationshipType, Long> {

    List<PersonRelationshipType> findByIsActiveTrueOrderBySortOrderAscNameAsc();

    Optional<PersonRelationshipType> findByCode(String code);
}
