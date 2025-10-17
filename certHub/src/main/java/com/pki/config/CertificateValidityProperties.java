package com.pki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "pki.certificate.validity")
@Data
public class CertificateValidityProperties {
    private int root = 3650;           // Default: 10 years
    private int intermediate = 1825;   // Default: 5 years
    private int leaf = 365;            // Default: 1 year
}

