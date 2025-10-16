package com.pki.service;

import com.pki.config.CertConfig;
import com.pki.config.PKIProperties;
import com.pki.dto.CertRequestDTO;
import com.pki.dto.CertResponseDTO;
import com.pki.exception.CertificateValidationException;
import com.pki.generator.KeyCertGenerator;
import com.pki.model.CertificateType;
import com.pki.model.IntermediateCA;
import com.pki.model.ServiceCertificate;
import com.pki.repository.IntermediateCARepository;
import com.pki.repository.ServiceCertificateRepository;
import com.pki.util.DNBuilder;
import com.pki.util.PasswordUtil;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class LeafCertificateService {
    @Autowired
    private final KeyCertGenerator generator;
    private final IntermediateCARepository intermediateRepo;
    private final ServiceCertificateRepository serviceCertRepo;

    private final PKIProperties pkiProperties;

    public LeafCertificateService(KeyCertGenerator generator,
                                  IntermediateCARepository intermediateRepo,
                                  ServiceCertificateRepository serviceCertRepo, PKIProperties pkiProperties) {
        this.generator = generator;
        this.intermediateRepo = intermediateRepo;
        this.serviceCertRepo = serviceCertRepo;
        this.pkiProperties = pkiProperties;
    }

    @Transactional
    public CertResponseDTO issueCertificate(CertRequestDTO req) throws Exception {
        IntermediateCA activeCA = intermediateRepo.findByStatus("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active Intermediate CA"));

//      KeyPair keyPair = generator.generateRSAKeyPair(2048);

        KeyPair keyPair = generator.generateKeyPair( req.getKeyAlg(), req.getKeySize(), req.getCurveName() );

        PKIProperties.SubjectDN subjectDN =  pkiProperties.getSubject();

        if( req.getCountry() != null && !req.getCountry().isEmpty() ){
            subjectDN.setCountry(req.getCountry());
        }

        if( req.getOrganization() != null && !req.getOrganization().isEmpty() ){
            subjectDN.setOrganization(req.getOrganization());
        }

        X500Name subject = DNBuilder.buildDN(req.getCommonName(), subjectDN);

        System.out.println("leaf Key algorithm: " + keyPair.getPrivate().getAlgorithm());
        System.out.println("leaf Key class: " + keyPair.getPrivate().getClass().getName());

        PrivateKey intKey = generator.loadPrivateKeyFromPem(activeCA.getPrivateKeyPem());
        X509Certificate intCert = generator.loadCertificateFromPem(activeCA.getCertificatePem());

        System.out.println("Intermediate Key algorithm: " + intKey.getAlgorithm());
        System.out.println("Intermediate Key class: " + intKey.getClass().getName());


        // Use per-request SANs if provided, else fall back to default from application.yml
        List<String> sanOverrides = req.getSans();
        List<String> sanList = (sanOverrides != null && !sanOverrides.isEmpty())
                ? sanOverrides
                : pkiProperties.getDefaultSan();

        Integer validityDays = req.getValidDays();
        int validityDaysValue = (validityDays != null) ? validityDays : generator.getValidityDays(CertificateType.LEAF);

        // Validate that the leaf certificate does not outlive the intermediate CA
        Date now = new Date();
        Date leafExpiry = Date.from(now.toInstant().plus(Duration.ofDays(validityDaysValue)));
        Date intermediateExpiry = intCert.getNotAfter();

        if (leafExpiry.after(intermediateExpiry)) {
            long maxAllowedDays = ChronoUnit.DAYS.between(
                    now.toInstant(), intermediateExpiry.toInstant()
            );

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("requestedDays", validityDaysValue);
            errorDetails.put("maxAllowedDays", maxAllowedDays);
            errorDetails.put("intermediateCAExpiry", intermediateExpiry.toString());

            throw new CertificateValidationException(
                    "Requested validity exceeds intermediate CA's remaining validity",
                    Collections.unmodifiableMap(errorDetails)
            );
        }

        PKCS10CertificationRequest csr = null;

        boolean isKeyAgreementOnly = "X25519".equalsIgnoreCase(req.getKeyAlg())
                                    || "X448".equalsIgnoreCase(req.getKeyAlg())
                                    || "DH".equalsIgnoreCase(req.getKeyAlg());
        if (!isKeyAgreementOnly) {
            csr = generator.generateCSR(keyPair, subject, req.getSignAlg());
        }else{
            // for DH, X25519 and X448 signing alg and curvename ignored as this is not required for
            // key exchange algorithm like above two. these three are only used for key generation,
            // not for certificate signing. keySize is used only for DH

            req.setSignAlg("");
            req.setCurveName("");
        }

        X509Certificate leafCert;
        if (isKeyAgreementOnly) {
            // Build manually from key pair (no CSR)
            leafCert = generator.generateKeyAgreementCertificate(
                    keyPair.getPublic(), subject, intKey, intCert,
                    validityDaysValue, activeCA.getSignAlg(), sanList
            );
        } else {
            // Use existing CSR flow
            // new leaf csr is signed by current active intermediate ca's private key and signing algorithm
            leafCert = generator.generateCertificateFromCSR(
                    csr, intKey, intCert, validityDaysValue, activeCA.getSignAlg(), sanList, intCert
            );

        }


        String chainPem = generator.toPemString(leafCert) + "\n" + activeCA.getCertificatePem();

        ServiceCertificate entity = new ServiceCertificate();
        entity.setServiceName(req.getServiceName());
        entity.setIntermediateCAId(activeCA.getId());
        entity.setPrivateKeyPem(generator.toPemString(keyPair.getPrivate()));
        entity.setCertificatePem(generator.toPemString(leafCert));
        entity.setCsrPem( !isKeyAgreementOnly ? generator.toPemString(csr) : "");
        entity.setChainPem(chainPem);
        entity.setIssuedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(req.getValidDays()));

        entity.setKeyAlg(req.getKeyAlg());
        entity.setKeySize(req.getKeySize());
        entity.setCurveName(req.getCurveName());
        entity.setSignAlg(req.getSignAlg());

        serviceCertRepo.save(entity);

        String storePassword = PasswordUtil.generateStrongPassword();

        String pkcs12Base64 = generator.generatePKCS12Base64(
                req.getServiceName(), keyPair.getPrivate(), leafCert, new X509Certificate[]{intCert}, storePassword.toCharArray()
        );

        CertResponseDTO resp = new CertResponseDTO();
        resp.setPrivateKeyPem(generator.toPemString(keyPair.getPrivate()));
        resp.setCertificatePem(generator.toPemString(leafCert));
        resp.setChainPem(chainPem);
        resp.setCsrPem( !isKeyAgreementOnly ? generator.toPemString(csr) : null );
        resp.setKeystoreBase64(pkcs12Base64);
        resp.setKeystorePassword(storePassword);

        return resp;
    }
}

