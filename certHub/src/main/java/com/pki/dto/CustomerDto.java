package com.pki.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
//    private String productId;

    @JsonProperty("firstName")
    private String firstName;
    
    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty( "nid")
    private String nid;

    @JsonProperty( "email")
    private String email;

    @JsonProperty( "fatherName")
    private String fatherName;

    @JsonProperty( "motherName")
    private String motherName;

    @JsonProperty( "presentAddress")
    private String presentAddress;

    @JsonProperty( "permanentAddress")
    private String permanentAddress;

}
