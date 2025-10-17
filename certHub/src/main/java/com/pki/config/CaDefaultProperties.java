package com.pki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ca.defaults")
@Data
public class CaDefaultProperties {

    private CertConfig root = new CertConfig();
    private CertConfig intermediate = new CertConfig();
    private CertConfig leaf = new CertConfig();



}