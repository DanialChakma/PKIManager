package com.pki.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "pki")
public class PKIProperties  {

    private List<String> defaultSan;
    private SubjectDN subject = new SubjectDN();


//    public List<String> getDefaultSan() { return defaultSan; }
//    public void setDefaultSan(List<String> defaultSan) { this.defaultSan = defaultSan; }


    @Data
    public static class SubjectDN {
        private String commonName;
        private String organization;
        private String organizationalUnit;
        private String country;
        private String state;
        private String locality;
        private String emailAddress;
        private String domainComponent;
        private String serialNumber;
        private String givenName;
        private String surname;
        private String title;
        private String initials;
        private String uid;
    }

}
