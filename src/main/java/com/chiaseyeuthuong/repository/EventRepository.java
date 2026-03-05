package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findBySlug(String slug);

    long countByStatus(EEventStatus status);

    @Query("""
                SELECT e FROM Event e
                WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<Event> searchByName(PageRequest pageRequest, String search);
}
