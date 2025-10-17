package com.pki.config;

import lombok.Data;

@Data
public class CertConfig {
    private String keyAlg;
    private Integer keySize;     // Nullable for Ed25519
    private String curveName;    // For EC
    private String signAlg;
}
