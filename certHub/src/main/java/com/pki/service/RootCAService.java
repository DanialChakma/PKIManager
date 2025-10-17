package com.pki.service;

import com.pki.config.CertConfig;
import com.pki.config.PKIProperties;
import com.pki.generator.KeyCertGenerator;
import com.pki.model.CertificateType;
import com.pki.model.RootCA;
import com.pki.repository.RootCARepository;
import com.pki.util.DNBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.bouncycastle.asn1.x500.X500Name;
import javax.transaction.Transactional;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

@Service
public class RootCAService {

    @Autowired
    private final KeyCertGenerator generator;

    private final RootCARepository rootRepo;

    private final PKIProperties pkiProperties;

    public RootCAService(RootCARepository rootRepo, KeyCertGenerator generator, PKIProperties pkiProperties) {
        this.rootRepo = rootRepo;
        this.generator = generator;
        this.pkiProperties = pkiProperties;
    }

    @Transactional
    public RootCA createRootCA(String alias, Integer validDays, CertConfig certConfig) throws Exception {
        // KeyPair kp = generator.generateRSAKeyPair(4096);
        String signAlg = certConfig.getSignAlg();
        KeyPair kp = generator.generateKeyPair(certConfig.getKeyAlg(), certConfig.getKeySize(), certConfig.getCurveName());

        int certValidityDays = validDays != null ? validDays : generator.getValidityDays(CertificateType.ROOT);
        //🧩 Build DN dynamically from config
        X500Name subject = DNBuilder.buildDN(alias, pkiProperties.getSubject());



        X509Certificate cert = generator.generateSelfSignedCertificate(kp, subject, certValidityDays, signAlg, pkiProperties.getDefaultSan() );

        RootCA root = new RootCA();
        root.setAlias(alias);
        root.setStatus("STANDBY");
        root.setCreatedAt(LocalDateTime.now());
        root.setExpiresAt(LocalDateTime.now().plusDays(certValidityDays));
        root.setPrivateKeyPem(generator.toPemString(kp.getPrivate()));
        root.setCertificatePem(generator.toPemString(cert));
        root.setKeyAlg(certConfig.getKeyAlg());
        root.setSignAlg(certConfig.getSignAlg());
        root.setKeySize(certConfig.getKeySize());
        root.setCurveName(certConfig.getCurveName());

        return rootRepo.save(root);
    }

    @Transactional
    public void activateRootCA(Long rootId) {
        // Deactivate current ACTIVE root
        rootRepo.findByStatus("ACTIVE").ifPresent(r -> {
            r.setStatus("RETIRED");
            rootRepo.save(r);
        });

        RootCA newRoot = rootRepo.findById(rootId)
                .orElseThrow(() -> new IllegalStateException("Root CA not found for id: " + rootId));

        newRoot.setStatus("ACTIVE");
        rootRepo.save(newRoot);
    }
}

