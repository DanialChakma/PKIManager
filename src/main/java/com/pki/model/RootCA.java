package com.pki.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "root_ca")
public class RootCA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "alias", nullable = false, unique = true, length = 100)
    private String alias;

    @Column(name = "status", nullable = false, length = 20) // ACTIVE, STANDBY, RETIRED
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Lob
    @Column(name = "private_key_pem", nullable = false)
    private String privateKeyPem;

    @Lob
    @Column(name = "certificate_pem", nullable = false)
    private String certificatePem;

    @Lob
    @Column(name = "cross_signed_cert_pem")
    private String crossSignedCertPem; // optional for trust continuity

    @Column(name = "key_alg", nullable = false)
    private String keyAlg;

    @Column(name = "key_size")
    private Integer keySize;

    @Column(name = "curve_name")
    private String curveName;

    @Column(name = "sign_alg")
    private String signAlg;

}

