package com.pki.generator;

import com.pki.config.CertificateValidityProperties;
import com.pki.model.CertificateType;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

@Component
public class KeyCertGenerator {

    private final CertificateValidityProperties validityProperties;

    public void generate() throws Exception {
        // Config
        String commonName = "example.local";
        String organization = "Example Org";
        String country = "US";
        int keySize = 2048;
        int validDays = 365;
        String keystoreFile = "keystore.p12";
        char[] keystorePassword = "changeit".toCharArray();
        String keyAlias = "mykey";

        // Generate KeyPair
        KeyPair keyPair = this.generateRSAKeyPair(keySize);

        // Build subject DN
        String subjectDN = String.format("CN=%s, O=%s, C=%s", commonName, organization, country);
        X500Name subject = new X500Name(subjectDN);

        // Create CSR (PKCS#10)
        PKCS10CertificationRequest csr = this.generateCSR(keyPair, subject, "SHA256withRSA");

        // Write CSR to PEM
        this.writePem("csr.pem", csr);

        // Create a self-signed certificate (simple example)
        X509Certificate cert = this.generateSelfSignedCertificate(keyPair, subject, validDays, "SHA256withRSA", Arrays.asList("localhost", "127.0.0.1") );

        // Write certificate to PEM
        this.writePem("cert.pem", cert);

        // Store private key + certificate into PKCS12 keystore
        this.storeToPKCS12(keystoreFile, keystorePassword, keyAlias, keyPair.getPrivate(), cert);

        System.out.println("Generated files: csr.pem, cert.pem, " + keystoreFile);
    }

    public KeyCertGenerator(CertificateValidityProperties validityProperties) {
        this.validityProperties = validityProperties;
    }

    public int getValidityDays(CertificateType type) {
        switch (type) {
            case ROOT:
                return validityProperties.getRoot();
            case INTERMEDIATE:
                return validityProperties.getIntermediate();
            case LEAF:
                return validityProperties.getLeaf();
            default:
                throw new IllegalArgumentException("Unknown certificate type: " + type);
        }
    }

    public KeyPair generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        kpGen.initialize(keySize, new SecureRandom());
        return kpGen.generateKeyPair();
    }

    public KeyPair generateKeyPair(String keyAlgorithm, Integer keySize, String curveName) throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator;

        // Add BouncyCastle provider if needed
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        switch (keyAlgorithm.toUpperCase()) {
            case "RSA":
            case "DSA":
                keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm);
                keyPairGenerator.initialize(keySize, new SecureRandom());
                break;

            case "EC":
                keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
                ECGenParameterSpec ecSpec = new ECGenParameterSpec(curveName); // e.g., "secp384r1"
                keyPairGenerator.initialize(ecSpec, new SecureRandom());
                break;

            case "ED25519":
            case "ED448":
                keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm, "BC");
//                keyPairGenerator.initialize(new SecureRandom());
                break;
            case "X25519":
            case "X448":
                keyPairGenerator = KeyPairGenerator.getInstance(keyAlgorithm, "BC");
                // No initialization needed for X25519/X448
                break;

            case "DH":
                keyPairGenerator = KeyPairGenerator.getInstance("DH", "BC");
                // You can use predefined DH parameters or generate them securely
                // Here's a common example with 2048-bit DH parameters
                keyPairGenerator.initialize(keySize, new SecureRandom());
                break;

            default:
                throw new IllegalArgumentException("Unsupported key algorithm: " + keyAlgorithm);
        }

        return keyPairGenerator.generateKeyPair();
    }

    public KeyPair generateKeyPair(String keyAlgorithm, int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance(keyAlgorithm);
        kpGen.initialize(keySize, new SecureRandom());
        return kpGen.generateKeyPair();
    }

    public  PKCS10CertificationRequest generateCSR(KeyPair keyPair, X500Name subject, String sigAlg)
            throws OperatorCreationException {
        // Build CSR
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(sigAlg);
        ContentSigner signer = csBuilder.build(keyPair.getPrivate());
        return p10Builder.build(signer);
    }


    public X509Certificate generateSelfSignedCertificate(KeyPair keyPair, X500Name subject, int days, String sigAlg, List<String> sanList)
            throws Exception {

        // Validity
        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(notBefore);
        cal.add(Calendar.DAY_OF_YEAR, days);
        Date notAfter = cal.getTime();

        // Serial
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        // Builder
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        // Add BasicConstraints: not a CA
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // (Optional) add more extensions here (subjectAltName, keyUsage, etc.)
//        addCertificateExtensions(certBuilder,  sanList);

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).build(keyPair.getPrivate());
        JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        return certConverter.getCertificate(certBuilder.build(signer));
    }

    public void writePem(String filename, Object obj) throws IOException {
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(filename))) {
            pemWriter.writeObject(obj);
        }
    }

    public void storeToPKCS12(String filename, char[] password, String alias, PrivateKey privateKey, X509Certificate cert)
            throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        java.security.cert.Certificate[] chain = new Certificate[] { cert };
        ks.setKeyEntry(alias, privateKey, password, chain);

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            ks.store(fos, password);
        }
    }

    /**
     * Generate certificate for an existing public key signed by a CA (cross-sign)
     */
    public X509Certificate generateCertificateForPublicKey(PublicKey publicKey,
                                                           X500Name subject,
                                                           PrivateKey caKey,
                                                           X509Certificate caCert,
                                                           int validDays,
                                                           String sigAlg, List<String> sanList) throws Exception {
        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(notBefore);
        cal.add(Calendar.DAY_OF_YEAR, validDays);
        Date notAfter = cal.getTime();

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                caCert,           // issuer
                serial,
                notBefore,
                notAfter,
                subject,          // subject
                publicKey         // existing public key
        );


        // Add extensions using helper
        // Basic constraints: leaf certificate
        addCertificateExtensions(certBuilder, sanList);

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).build(caKey);

        return new JcaX509CertificateConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                .getCertificate(certBuilder.build(signer));
    }

    public X509Certificate loadCertificateFromPem(String pem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj instanceof org.bouncycastle.cert.X509CertificateHolder) {
                org.bouncycastle.cert.X509CertificateHolder certHolder = (org.bouncycastle.cert.X509CertificateHolder) obj;
                return new JcaX509CertificateConverter()
                        .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                        .getCertificate(certHolder);
            } else {
                throw new IllegalArgumentException("Invalid certificate PEM");
            }
        }
    }


    public PrivateKey loadPrivateKeyFromPem(String pem) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            if (obj instanceof PEMKeyPair) {
                return converter.getKeyPair((PEMKeyPair) obj).getPrivate();
            } else if (obj instanceof PrivateKeyInfo) {
                return converter.getPrivateKey((PrivateKeyInfo) obj);
            } else {
                throw new IllegalArgumentException("Invalid or unsupported private key PEM format");
            }
        }
    }

//    public String toPemString(Object obj) throws Exception {
//        StringWriter stringWriter = new StringWriter();
//        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
//            pemWriter.writeObject(obj);
//        }
//        return stringWriter.toString();
//    }

    // Determine PEM label based on key encoding
    private String determinePemType(PrivateKey privateKey, byte[] encoded, String format) {
        if ("PKCS#8".equalsIgnoreCase(format)) {
            if (isEncryptedPkcs8(encoded)) {
                return "ENCRYPTED PRIVATE KEY";
            } else {
                return "PRIVATE KEY";
            }
        } else if ("PKCS#1".equalsIgnoreCase(format) || isPkcs1RsaKey(encoded)) {
            return "RSA PRIVATE KEY";
        } else if (isSec1EcKey(encoded)) {
            return "EC PRIVATE KEY";
        } else {
            // Fallback if the format is unknown
            return "PRIVATE KEY";
        }
    }

    // Detect encrypted PKCS#8
    private boolean isEncryptedPkcs8(byte[] encoded) {
        try (ASN1InputStream asn1Stream = new ASN1InputStream(encoded)) {
            ASN1Primitive asn1 = asn1Stream.readObject();
            if (asn1 instanceof ASN1Sequence) {
                ASN1Sequence seq = (ASN1Sequence) asn1;
                // Check if the first element in sequence is an encryption OID
                ASN1Encodable first = seq.getObjectAt(0);
                if (first instanceof ASN1ObjectIdentifier) {
                    ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) first;
                    return oid.getId().startsWith("1.2.840.113549.1.5"); // AES or other encryption OIDs
                }
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    // Detect PKCS#1 RSA private key
    private boolean isPkcs1RsaKey(byte[] encoded) {
        try (ASN1InputStream asn1Stream = new ASN1InputStream(encoded)) {
            ASN1Primitive primitive = asn1Stream.readObject();
            if (primitive instanceof ASN1Sequence) {
                ASN1Sequence seq = (ASN1Sequence) primitive;
                return seq.size() == 9; // PKCS#1 typically has 9 elements (modulus, exponent, etc.)
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    // Detect SEC1 EC private key
    private boolean isSec1EcKey(byte[] encoded) {
        try (ASN1InputStream asn1Stream = new ASN1InputStream(encoded)) {
            ASN1Primitive asn1 = asn1Stream.readObject();
            if (asn1 instanceof ASN1Sequence) {
                ASN1Sequence seq = (ASN1Sequence) asn1;
                return seq.size() == 2; // SEC1 should have two elements: ECParams + PrivateKey
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }

    public String toPemString(Object obj) throws IOException {
        StringWriter stringWriter = new StringWriter();
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            if (obj instanceof PrivateKey) {
                // Instead of wrapping with PrivateKeyInfo manually,
                // use PemObject directly
                PrivateKey privateKey = (PrivateKey) obj;
                byte[] encoded = privateKey.getEncoded();
                String format = privateKey.getFormat(); // Usually "PKCS#8" or null
                String pemType = determinePemType(privateKey, encoded, format);
                PemObject pemObject = new PemObject(pemType, encoded);
                pemWriter.writeObject(pemObject);
            } else if (obj instanceof PublicKey) {
                byte[] encoded = ((PublicKey) obj).getEncoded();
                PemObject pemObject = new PemObject("PUBLIC KEY", encoded);
                pemWriter.writeObject(pemObject);
            } else {
                pemWriter.writeObject(obj);
            }
        }
        return stringWriter.toString();
    }


    private static void addCertificateExtensions(X509v3CertificateBuilder certBuilder, List<String> sanList) throws Exception {
        // Basic constraints: leaf certificate
        certBuilder.addExtension(Extension.basicConstraints, false, new BasicConstraints(false));

        // Subject Alternative Names
        if (sanList != null && !sanList.isEmpty()) {
            GeneralName[] names = sanList.stream()
                    .map(name -> {
                        if (name.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) { // IP
                            return new GeneralName(GeneralName.iPAddress, name);
                        } else { // DNS
                            return new GeneralName(GeneralName.dNSName, name);
                        }
                    })
                    .toArray(GeneralName[]::new);

            GeneralNames subjectAltNames = new GeneralNames(names);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        }
    }


    public X509Certificate generateCertificateFromCSR(PKCS10CertificationRequest csr,
                                                      PrivateKey caKey,
                                                      X509Certificate caCert,
                                                      int validDays,
                                                      String sigAlg,
                                                      List<String> sanList,
                                                      X509Certificate oldCert ) throws Exception {

        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(notBefore);
        cal.add(Calendar.DAY_OF_YEAR, validDays);
        Date notAfter = cal.getTime();

        String keyAlg = caKey.getAlgorithm();  // e.g., "RSA", "EC", "Ed25519", etc.
        KeyFactory kf = KeyFactory.getInstance(keyAlg, "BC");

        PKCS8EncodedKeySpec pkcs8 = new PKCS8EncodedKeySpec(caKey.getEncoded());
        PrivateKey cleanKey = kf.generatePrivate(pkcs8);


        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        // Extract issuer X500Name from CA certificate
        X500Name issuer = new X500Name(caCert.getSubjectX500Principal().getName());

        // Convert CSR public key to java.security.PublicKey
        PublicKey csrPubKey = new JcaPEMKeyConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                .getPublicKey(csr.getSubjectPublicKeyInfo());

        // Build certificate using CSR's subject and public key
//        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
//                issuer,                     // issuer X500Name
//                serial,
//                notBefore,
//                notAfter,
//                csr.getSubject(), // subject from CSR
//                csrPubKey // public key from CSR
//        );


        SubjectPublicKeyInfo subjectPublicKeyInfo = csr.getSubjectPublicKeyInfo();
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, csr.getSubject(), subjectPublicKeyInfo
        );

        // Add extensions using helper
        // Basic constraints: leaf certificate
        addCertificateExtensions(certBuilder, sanList);

        if(oldCert != null){
            copyExtensions(oldCert, certBuilder, false);
        }

//        System.out.println("Intermediate Key algorithm: " + cleanKey.getAlgorithm());
//        System.out.println("Intermediate Key class: " + cleanKey.getClass().getName());
//        System.out.println("Intermediate Signing algorithm: " + sigAlg);

        System.out.println("Clean Key algorithm: " + cleanKey.getAlgorithm());
        System.out.println("Clean Key class: " + cleanKey.getClass().getName());
        System.out.println("Clean Key format: " + cleanKey.getFormat());
        System.out.println("cleanKey.getEncoded().length = " + Arrays.toString(cleanKey.getEncoded()).length() );


        System.out.println("Signing Algorithm: " + sigAlg);

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg).build(cleanKey);
        System.out.println("Has reached after JcaContentSignerBuilder: " + "yes");

        try{
            X509CertificateHolder certHolder = certBuilder.build(signer);
            System.out.println("Built holder: " + certHolder);
            X509Certificate certificate =  new JcaX509CertificateConverter()
                    .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                    .getCertificate(certHolder);
            System.out.println("Certificate Created: " + "yes");
            return certificate;
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Built holder: " + e);
            throw e;
        }


    }


    public X509Certificate generateKeyAgreementCertificate(PublicKey publicKey,
                                                           X500Name subject,
                                                           PrivateKey caKey,
                                                           X509Certificate caCert,
                                                           int validDays,
                                                           String sigAlg,
                                                           List<String> sanList) throws Exception {

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + validDays * 86400000L);

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(publicKey.getEncoded());
        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                new X500Name(caCert.getSubjectX500Principal().getName()),
                BigInteger.valueOf(System.currentTimeMillis()),
                notBefore,
                notAfter,
                subject,
                subjectPublicKeyInfo
        );

        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyAgreement));

        // Optionally add SANs
        if (sanList != null && !sanList.isEmpty()) {
            GeneralName[] generalNames = sanList.stream()
                    .map(name -> new GeneralName(GeneralName.dNSName, name))
                    .toArray(GeneralName[]::new);
            certBuilder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(generalNames));
        }

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .setProvider("BC")
                .build(caKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }


    public String generatePKCS12Base64(String alias,
                                       PrivateKey key,
                                       X509Certificate cert,
                                       X509Certificate[] chain,
                                       char[] password) throws Exception {

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, key, password, chain);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ks.store(bos, password);

        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }

    public X509Certificate crossSignCertificate(X509Certificate oldCert,
                                                PrivateKey signerKey,
                                                X509Certificate signerCert,
                                                String sigAlg) throws Exception {

        X500Name issuer = new X500Name(signerCert.getSubjectX500Principal().getName());
        X500Name subject = new X500Name(oldCert.getSubjectX500Principal().getName());
        PublicKey subjectKey = oldCert.getPublicKey();

        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(notBefore);
        cal.add(Calendar.YEAR, 5);
        Date notAfter = cal.getTime();

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, subjectKey
        );

        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));

        ContentSigner signer = new JcaContentSignerBuilder(sigAlg)
                .build(signerKey);

        return new JcaX509CertificateConverter()
                .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                .getCertificate(builder.build(signer));
    }


    public static void copyExtensions(X509Certificate oldCert,
                                      X509v3CertificateBuilder certBuilder,
                                      boolean includeAuthorityExtensions) throws Exception {

        X509CertificateHolder oldHolder = new X509CertificateHolder(oldCert.getEncoded());
        Extensions oldExtensions = oldHolder.getExtensions();

        @SuppressWarnings("unchecked")
        Enumeration<ASN1ObjectIdentifier> e = oldExtensions.oids();
        while (e.hasMoreElements()) {
            ASN1ObjectIdentifier oid = e.nextElement();
            Extension ext = oldExtensions.getExtension(oid);

            // Skip AuthorityKeyIdentifier and SubjectKeyIdentifier unless requested
            if (!includeAuthorityExtensions && (
                    oid.equals(Extension.authorityKeyIdentifier) ||
                            oid.equals(Extension.subjectKeyIdentifier))) {
                continue;
            }

            // Skip basic constraints (usually regenerated per cert)
            if (oid.equals(Extension.basicConstraints)) {
                continue;
            }

            // ❗ Skip SAN (Subject Alternative Name) to avoid "already added"
            if (oid.equals(Extension.subjectAlternativeName)) {
                continue;
            }

            // Finally, add the extension
            if (certBuilder != null) {
                certBuilder.addExtension(oid, ext.isCritical(), ext.getParsedValue());
            }
        }
    }


}

