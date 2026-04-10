package com.chiaseyeuthuong.repository;

import com.chiaseyeuthuong.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
