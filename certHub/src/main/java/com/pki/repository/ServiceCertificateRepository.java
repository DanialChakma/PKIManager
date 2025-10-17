package com.pki.repository;

import com.pki.model.ServiceCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceCertificateRepository extends JpaRepository<ServiceCertificate, Long> {
    Optional<ServiceCertificate> findByServiceName(String serviceName);
}
