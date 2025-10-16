package com.pki.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertResponseDTO {

    @JsonProperty("private_key_pem")
    private String privateKeyPem;

    @JsonProperty("certificate_pem")
    private String certificatePem;

    @JsonProperty("chain_pem")
    private String chainPem;

    @JsonProperty("csr_pem")
    private String csrPem;

    @JsonProperty("keystore_base64")
    private String keystoreBase64;

    @JsonProperty("keystore_password")
    private String keystorePassword;
}


