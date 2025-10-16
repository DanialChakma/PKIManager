package com.pki.model;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "service_certificates")
public class ServiceCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "intermediate_ca_id", nullable = false)
    private Long intermediateCAId;

    @Lob
    @Column(name = "private_key_pem", nullable = false)
    private String privateKeyPem;

    @Lob
    @Column(name = "certificate_pem", nullable = false)
    private String certificatePem;

    @Lob
    @Column(name = "csr_pem", nullable = false)
    private String csrPem;

    @Lob
    @Column(name = "chain_pem", nullable = false)
    private String chainPem;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "key_alg", nullable = false)
    private String keyAlg;

    @Column(name = "key_size")
    private Integer keySize;

    @Column(name = "curve_name")
    private String curveName;

    @Column(name = "sign_alg")
    private String signAlg;

}

