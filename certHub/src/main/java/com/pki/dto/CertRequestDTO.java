package com.pki.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertRequestDTO {

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("common_name")
    private String commonName;

    @JsonProperty("organization")
    private String organization;

    @JsonProperty("country")
    private String country;

    @JsonProperty("valid_days")
    private Integer validDays = 365;

    @JsonProperty("sans")
    private List<String> sans;

    @JsonProperty("key_alg")
    private String keyAlg;
    @JsonProperty("key_size")
    private Integer keySize;
    @JsonProperty("curve_name")
    private String curveName;
    @JsonProperty("sign_alg")
    private String signAlg;

}

