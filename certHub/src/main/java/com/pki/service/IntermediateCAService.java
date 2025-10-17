package com.pki.service;

import com.pki.config.CaDefaultProperties;
import com.pki.config.CertConfig;
import com.pki.config.CertificateValidityProperties;
import com.pki.config.PKIProperties;
import com.pki.controller.CertificateController;
import com.pki.dto.CAStatusResponse;
import com.pki.generator.KeyCertGenerator;
import com.pki.model.CertificateType;
import com.pki.model.RootCA;
import com.pki.repository.RootCARepository;
import com.pki.util.DNBuilder;
import org.springframework.stereotype.Service;
import com.pki.model.IntermediateCA;
import com.pki.repository.IntermediateCARepository;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.transaction.Transactional;
import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class IntermediateCAService {

    private final IntermediateCARepository intRepo;
    private final RootCARepository rootRepo;

    private final KeyCertGenerator generator;
    private final PKIProperties pkiProperties;
    private final CertificateValidityProperties defaultValidity;
    private  final CaDefaultProperties caDefaultProperties;
    public IntermediateCAService(IntermediateCARepository intRepo,
                                 RootCARepository rootRepo,
                                 KeyCertGenerator generator,
                                 PKIProperties pkiProperties,
                                 CertificateValidityProperties defaultValidity,
                                 CaDefaultProperties caDefaultProperties
    ) {
        this.intRepo = intRepo;
        this.rootRepo = rootRepo;
        this.generator = generator;
        this.pkiProperties = pkiProperties;
        this.defaultValidity = defaultValidity;
        this.caDefaultProperties = caDefaultProperties;
    }

    @Transactional
    public IntermediateCA createIntermediateCA(String alias, Long rootId, Integer validDays, List<String> sanList, CertConfig certConfig) throws Exception, IOException {
        RootCA root = rootRepo.findById(rootId).orElseThrow( ()-> new IllegalStateException("No Root CA found") );

        //KeyPair kp = generator.generateRSAKeyPair(4096);

        KeyPair kp = generator.generateKeyPair(certConfig.getKeyAlg(), certConfig.getKeySize(), certConfig.getCurveName());

        int certValidityDays = validDays != null ? validDays : generator.getValidityDays(CertificateType.INTERMEDIATE);
        // CSR for intermediate signed by root
        PKIProperties.SubjectDN subjectDN =  pkiProperties.getSubject();
        X500Name subject = DNBuilder.buildDN(alias, subjectDN);

        PKCS10CertificationRequest csr = generator.generateCSR(kp, subject, certConfig.getSignAlg());

        X509Certificate rootCert = generator.loadCertificateFromPem(root.getCertificatePem());
        PrivateKey rootKey = generator.loadPrivateKeyFromPem(root.getPrivateKeyPem());

        X509Certificate intCert = generator.generateCertificateFromCSR(csr, rootKey, rootCert, certValidityDays, certConfig.getSignAlg(), sanList, null);

        IntermediateCA intCA = new IntermediateCA();
        intCA.setAlias(alias);
        intCA.setRootCAId(rootId);
        intCA.setStatus("STANDBY");
        intCA.setCreatedAt(LocalDateTime.now());
        intCA.setExpiresAt(LocalDateTime.now().plusDays(certValidityDays));
        intCA.setPrivateKeyPem(generator.toPemString(kp.getPrivate()));
        intCA.setCertificatePem(generator.toPemString(intCert));

        intCA.setKeyAlg(certConfig.getKeyAlg());
        intCA.setSignAlg(certConfig.getSignAlg());
        intCA.setKeySize(certConfig.getKeySize());
        intCA.setCurveName(certConfig.getCurveName());

        return intRepo.save(intCA);
    }


    /**
     * Rotate the active Intermediate CA.
     * Generates new keypair, certificate signed by Root,
     * No cross-signs old intermediate needed to rotate active Intermediate CA.
     */

    @Transactional
    public IntermediateCA rotateActiveIntermediate() throws Exception {

//        defaultValidity.getIntermediate();
        CertConfig intConfig = caDefaultProperties.getIntermediate();
        int intCertValidityDays = defaultValidity.getIntermediate();

        // 1️⃣ Load active root and intermediate
        RootCA activeRoot = rootRepo.findByStatus("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active Root CA found"));
        IntermediateCA oldInt = intRepo.findByStatus("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active Intermediate CA found"));

        // 2️⃣ Generate new intermediate keypair
//        KeyPair newKeyPair = generator.generateRSAKeyPair(2048);
        KeyPair newKeyPair = generator.generateKeyPair(intConfig.getKeyAlg(), intConfig.getKeySize(), intConfig.getCurveName());

        // 3️⃣ Load Root CA private key and certificate
        PrivateKey rootKey = generator.loadPrivateKeyFromPem(activeRoot.getPrivateKeyPem());
        X509Certificate rootCert = generator.loadCertificateFromPem(activeRoot.getCertificatePem());
        X509Certificate oldIntCert = generator.loadCertificateFromPem(oldInt.getCertificatePem());

        // 4️⃣ Build new intermediate subject DN
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4);
        String newIntAlias = "ICA-" + datePart + "-" + randomPart;
        // 🧩 Build DN dynamically from config

        X500Name subject = DNBuilder.buildDN(newIntAlias, pkiProperties.getSubject());
//      X500Name subject = new X500Name("CN=" + newIntAlias + ", O=PKI, C=US");

        // 5️⃣ Generate CSR for the new intermediate
        PKCS10CertificationRequest csr = generator.generateCSR(newKeyPair, subject, intConfig.getSignAlg() );

        int validityDays = generator.getValidityDays(CertificateType.INTERMEDIATE);
        // 6️⃣ Sign CSR with Root CA to issue new intermediate certificate
        X509Certificate newIntCert = generator.generateCertificateFromCSR(
                csr,
                rootKey,
                rootCert,
                validityDays, // valid for 5 years
                activeRoot.getSignAlg(),
                pkiProperties.getDefaultSan(),
                oldIntCert
        );

        // 7️⃣ Retire the old intermediate
        oldInt.setStatus("RETIRED");
        intRepo.save(oldInt);

        // 8️⃣ Persist new active intermediate
        IntermediateCA newIntermediate = new IntermediateCA();
        newIntermediate.setAlias(newIntAlias);
        newIntermediate.setStatus("ACTIVE");
        newIntermediate.setRootCAId(activeRoot.getId());
        newIntermediate.setCreatedAt(LocalDateTime.now());
        newIntermediate.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        newIntermediate.setPrivateKeyPem(generator.toPemString(newKeyPair.getPrivate()));
        newIntermediate.setCertificatePem(generator.toPemString(newIntCert));
        newIntermediate.setKeyAlg(intConfig.getKeyAlg());
        newIntermediate.setKeySize(intConfig.getKeySize());
        newIntermediate.setCurveName(intConfig.getCurveName());
        newIntermediate.setSignAlg(intConfig.getSignAlg());
        intRepo.save(newIntermediate);

        return newIntermediate;
    }



    /**
     * Cross-sign all old intermediates with the new root
     */
    @Transactional
    public void crossSignOldIntermediates(Long newRootId, Long newIntermediateId) throws Exception {

        RootCA newRoot = rootRepo.findById(newRootId)
                .orElseThrow(() -> new IllegalStateException("New root CA not found"));

        // Pick intermediates that are active, retired, or expired within last 30 days
        LocalDateTime recentThreshold = LocalDateTime.now().minusDays(30);

        List<IntermediateCA> oldIntermediates = intRepo.findRelevantForCrossSign(
                Arrays.asList("ACTIVE", "RETIRED"),
                recentThreshold,
                newIntermediateId
        );

        for (IntermediateCA oldInt : oldIntermediates) {
            crossSignIntermediate(oldInt, newRoot);
        }
    }

    /**
     * Cross-sign a single intermediate CA with the new root
     */

    @Transactional
    public void crossSignIntermediate(IntermediateCA intermediate, RootCA newRoot) throws Exception {
        // Load old intermediate public key from stored certificate
        X509Certificate oldCert = generator.loadCertificateFromPem(intermediate.getCertificatePem());
        PublicKey oldPubKey = oldCert.getPublicKey();

        // 🧩 Build DN dynamically from config
        X500Name subject = DNBuilder.buildDN(intermediate.getAlias(), pkiProperties.getSubject());

        // Load new root private key & certificate
        PrivateKey rootKey = generator.loadPrivateKeyFromPem(newRoot.getPrivateKeyPem());
        X509Certificate rootCert = generator.loadCertificateFromPem(newRoot.getCertificatePem());

        // Generate cross-signed certificate directly
        X509Certificate crossSignedCert = generator.generateCertificateForPublicKey(
                oldPubKey,
                subject,
                rootKey,
                rootCert,
                defaultValidity.getIntermediate(),
                newRoot.getSignAlg(),
                null
        );

        intermediate.setCertificatePem(generator.toPemString(crossSignedCert));
        intRepo.save(intermediate);
    }


    @Transactional
    public void activateIntermediateCA(Long intermediateId) {
        // Deactivate current ACTIVE intermediate
        intRepo.findByStatus("ACTIVE").ifPresent(i -> {
            i.setStatus("RETIRED");
            intRepo.save(i);
        });

        IntermediateCA newInt = intRepo.findById(intermediateId).orElseThrow(()-> new IllegalStateException("No Intermediate CA found."));
        newInt.setStatus("ACTIVE");
        intRepo.save(newInt);
    }

    public CAStatusResponse getPkiChangeStatus() throws Exception {

        RootCA activeRoot = rootRepo.findByStatus("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active Root CA found"));
        IntermediateCA activeInt = intRepo.findByStatus("ACTIVE")
                .orElseThrow(() -> new IllegalStateException("No active Intermediate CA found"));

        X509Certificate rootCert = generator.loadCertificateFromPem(activeRoot.getCertificatePem());
        X509Certificate intCert = generator.loadCertificateFromPem(activeInt.getCertificatePem());

        String rootFingerprint = fingerprint(rootCert);
        String intFingerprint = fingerprint(intCert);

        return new CAStatusResponse(
                activeRoot.getAlias(),
                activeRoot.getCertificatePem(),
                rootFingerprint,
                activeInt.getAlias(),
                activeInt.getCertificatePem(),
                intFingerprint,
                activeInt.getCreatedAt().atOffset(ZoneOffset.UTC).toString()
        );
    }

    private static String fingerprint(X509Certificate cert) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

}

