package com.pki.util;


import com.pki.config.PKIProperties;
import org.bouncycastle.asn1.x500.X500Name;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DNBuilder {

    /**
     * Builds an X500Name dynamically from non-null SubjectDN fields.
     */
    public static X500Name buildDN(String commonNameOverride, PKIProperties.SubjectDN subjectDN) {
        Map<String, String> dnParts = new LinkedHashMap<>();

        dnParts.put("CN", commonNameOverride != null ? commonNameOverride : subjectDN.getCommonName());
        dnParts.put("O", subjectDN.getOrganization());
        dnParts.put("OU", subjectDN.getOrganizationalUnit());
        dnParts.put("C", subjectDN.getCountry());
        dnParts.put("ST", subjectDN.getState());
        dnParts.put("L", subjectDN.getLocality());
        dnParts.put("E", subjectDN.getEmailAddress());
        dnParts.put("DC", subjectDN.getDomainComponent());
        dnParts.put("SERIALNUMBER", subjectDN.getSerialNumber());
        dnParts.put("GIVENNAME", subjectDN.getGivenName());
        dnParts.put("SURNAME", subjectDN.getSurname());
        dnParts.put("T", subjectDN.getTitle());
        dnParts.put("INITIALS", subjectDN.getInitials());
        dnParts.put("UID", subjectDN.getUid());

        String dn = dnParts.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().trim().isEmpty())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));

        return new X500Name(dn);
    }

}

