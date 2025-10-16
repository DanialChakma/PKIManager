package com.pki.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RotateRequestDTO {

    @JsonProperty("new_root_alias")
    private String newRootAlias;
    @JsonProperty("new_root_key_alg")
    private String newRootKeyAlg;
    @JsonProperty("new_root_key_size")
    private Integer newRootKeySize;
    @JsonProperty("new_root_curve_name")
    private String newRootCurveName;
    @JsonProperty("new_root_sign_alg")
    private String newRootSignAlg;


    @JsonProperty("new_intermediate_alias")
    private String newIntermediateAlias;
    @JsonProperty("new_intermediate_key_alg")
    private String newIntKeyAlg;
    @JsonProperty("new_intermediate_key_size")
    private Integer newIntKeySize;
    @JsonProperty("new_intermediate_curve_name")
    private String newIntCurveName;
    @JsonProperty("new_intermediate_sign_alg")
    private String newIntSignAlg;

    @JsonProperty("root_valid_days")
    private Integer rootValidDays = 3650;

    @JsonProperty("intermediate_valid_days")
    private Integer intermediateValidDays = 1825;

    @JsonProperty("sans")
    private List<String> sans;
}


