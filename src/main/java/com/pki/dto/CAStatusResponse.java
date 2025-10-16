package com.pki.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CAStatusResponse {

    @JsonProperty("root_alias")
    private String rootAlias;

    @JsonProperty("root_cert_pem")
    private String rootCertPem;

    @JsonProperty("root_fingerprint")
    private String rootFingerprint;

    @JsonProperty("intermediate_alias")
    private String intermediateAlias;

    @JsonProperty("intermediate_cert_pem")
    private String intermediateCertPem;

    @JsonProperty("intermediate_fingerprint")
    private String intermediateFingerprint;

    @JsonProperty("last_rotated_at")
    private String lastRotatedAt;
}
