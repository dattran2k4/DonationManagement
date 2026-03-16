package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByKey(String key);

    List<SystemConfig> findByKeyIn(Collection<String> keys);
}
