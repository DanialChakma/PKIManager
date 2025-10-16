package com.pki.repository;

import com.pki.model.RootCA;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RootCARepository extends JpaRepository<RootCA, Long> {
    Optional<RootCA> findByStatus(String status);
}





