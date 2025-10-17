package com.pki.controller;

import com.pki.config.CaDefaultProperties;
import com.pki.config.CertConfig;
import com.pki.dto.CAStatusResponse;
import com.pki.dto.CertRequestDTO;
import com.pki.dto.CertResponseDTO;
import com.pki.dto.RotateRequestDTO;
import com.pki.exception.InvalidKeyAlgorithmCombinationException;
import com.pki.model.IntermediateCA;
import com.pki.model.RootCA;
import com.pki.repository.IntermediateCARepository;
import com.pki.service.IntermediateCAService;
import com.pki.service.LeafCertificateService;
import com.pki.service.RootCAService;
import com.pki.util.KeyAlgorithmValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ca")
public class CertificateController {

    private final RootCAService rootService;
    private final LeafCertificateService leafService;
    private final IntermediateCAService intermediateService;
    private final CaDefaultProperties caDefaultProperties;
    private final KeyAlgorithmValidator keyAlgorithmValidator;
    @Autowired
    private IntermediateCARepository intermediateRepo;

    public CertificateController(RootCAService rootService,
                                 LeafCertificateService leafService,
                                 IntermediateCAService intermediateService,
                                 CaDefaultProperties caDefaultProperties,
                                 KeyAlgorithmValidator keyAlgorithmValidator
    ) {
        this.rootService = rootService;
        this.leafService = leafService;
        this.intermediateService = intermediateService;
        this.caDefaultProperties = caDefaultProperties;
        this.keyAlgorithmValidator = keyAlgorithmValidator;
    }

    @PostMapping("/issue")
    public ResponseEntity<CertResponseDTO> issue(@RequestBody CertRequestDTO req) throws Exception {

        CertConfig defaultCertConfig = caDefaultProperties.getLeaf();

        String keyAlg = req.getKeyAlg();
        String signAlg = req.getSignAlg();
        Integer keySize = req.getKeySize();
        String curveName = req.getCurveName();

        if(keyAlg == null || keyAlg.isEmpty()){
            req.setKeyAlg(defaultCertConfig.getKeyAlg());
        }

        if( signAlg == null  || signAlg.isEmpty() ){
            req.setSignAlg(defaultCertConfig.getSignAlg());
        }

        if( curveName == null  || curveName.isEmpty() ){
            req.setCurveName(defaultCertConfig.getCurveName());
        }

        if( keySize == null  || keySize < 128 ){
            req.setKeySize(defaultCertConfig.getKeySize());
        }

        boolean isKeyAgreementOnly = "X25519".equalsIgnoreCase(req.getKeyAlg())
                || "X448".equalsIgnoreCase(req.getKeyAlg())
                || "DH".equalsIgnoreCase(req.getKeyAlg());

        if(isKeyAgreementOnly){
            // for keyAgreement key alg, sign alg and curve name is not required
            req.setSignAlg("");
            req.setCurveName("");
        }

        boolean isValid = keyAlgorithmValidator.isValidCombination(req.getKeyAlg(), req.getSignAlg());

        if( !isValid ){
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("keyAlg", req.getKeyAlg());
            errorDetails.put("signAlg", req.getSignAlg());
            throw new InvalidKeyAlgorithmCombinationException(
                    "Invalid combination: key algorithm and signing algorithm for Leaf Certificate",
                    errorDetails
            );
        }

        return ResponseEntity.ok(leafService.issueCertificate(req));
    }

    @PostMapping("/renew")
    public ResponseEntity<CertResponseDTO> renew(@RequestBody CertRequestDTO req) throws Exception {
        CertConfig defaultCertConfig = caDefaultProperties.getLeaf();

        String keyAlg = req.getKeyAlg();
        String signAlg = req.getSignAlg();
        Integer keySize = req.getKeySize();
        String curveName = req.getCurveName();

        if(keyAlg == null || keyAlg.isEmpty()){
            req.setKeyAlg(defaultCertConfig.getKeyAlg());
        }

        if( signAlg == null  || signAlg.isEmpty() ){
            req.setSignAlg(defaultCertConfig.getSignAlg());
        }

        if( curveName == null  || curveName.isEmpty() ){
            req.setCurveName(defaultCertConfig.getCurveName());
        }

        if( keySize == null  || keySize < 128 ){
            req.setKeySize(defaultCertConfig.getKeySize());
        }

        boolean isValid = keyAlgorithmValidator.isValidCombination(req.getKeyAlg(), req.getSignAlg());

        if( !isValid ){
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("keyAlg", req.getKeyAlg());
            errorDetails.put("signAlg", req.getSignAlg());
            throw new InvalidKeyAlgorithmCombinationException(
                    "Invalid combination: key algorithm and signing algorithm for Leaf Certificate",
                    errorDetails
            );
        }

        return ResponseEntity.ok(leafService.issueCertificate(req)); // same as issue for simplicity
    }

    @PostMapping("/rotate-intermediate")
    public ResponseEntity<?> rotateIntermediate() throws Exception {
        IntermediateCA newCA = intermediateService.rotateActiveIntermediate();
        return ResponseEntity.ok("New intermediate activated: " + newCA.getAlias());
    }

    private CertConfig getRootCertConfig(RotateRequestDTO req, CertConfig rootCertConfig){
        String rootKeyAlg = req.getNewRootKeyAlg();
        String rootSignAlg = req.getNewRootSignAlg();
        Integer rootKeySize = req.getNewRootKeySize();
        String rootCurveName = req.getNewRootCurveName();

        if( rootKeyAlg != null  && !rootKeyAlg.isEmpty() ){
            rootCertConfig.setKeyAlg(rootKeyAlg);
        }

        if( rootSignAlg != null  && !rootSignAlg.isEmpty() ){
            rootCertConfig.setSignAlg(rootSignAlg);
        }

        if( rootCurveName != null  && !rootCurveName.isEmpty() ){
            rootCertConfig.setCurveName(rootCurveName);
        }

        if( rootKeySize != null  && rootKeySize > 128 ){
            rootCertConfig.setKeySize(rootKeySize);
        }

        return rootCertConfig;
    }

    private CertConfig getIntCertConfig(RotateRequestDTO req, CertConfig intCertConfig){
        // intermediate ca properties
        String intKeyAlg = req.getNewIntKeyAlg();
        String intSignAlg = req.getNewIntSignAlg();
        Integer intKeySize = req.getNewIntKeySize();
        String intCurveName = req.getNewIntCurveName();

        if( intKeyAlg != null  && !intKeyAlg.isEmpty() ){
            intCertConfig.setKeyAlg(intKeyAlg);
        }

        if( intSignAlg != null  && !intSignAlg.isEmpty() ){
            intCertConfig.setSignAlg(intSignAlg);
        }

        if( intCurveName != null  && !intCurveName.isEmpty() ){
            intCertConfig.setCurveName(intCurveName);
        }

        if( intKeySize != null  && intKeySize > 128 ){
            intCertConfig.setKeySize(intKeySize);
        }

        return intCertConfig;
    }

    @PostMapping("/rotate")
    @Transactional
    public ResponseEntity<String> rotate(@RequestBody RotateRequestDTO req) throws Exception {

        Integer rootValidityDays = req.getRootValidDays();
        Integer intValidityDays = req.getIntermediateValidDays();

        // Validate
        if ( rootValidityDays == null || intValidityDays == null ) {
            throw new IllegalArgumentException("Validity days cannot be null");
        }

        if (rootValidityDays < intValidityDays) {
            throw new IllegalArgumentException(
                    String.format("Root CA validity (%d days) must be greater than or equal to Intermediate CA validity (%d days)",
                            rootValidityDays, intValidityDays)
            );
        }

        CertConfig rootCertConfig = caDefaultProperties.getRoot();
        CertConfig intCertConfig = caDefaultProperties.getIntermediate();

        rootCertConfig = getRootCertConfig(req, rootCertConfig);
        intCertConfig = getIntCertConfig(req, intCertConfig);

        boolean isValid = keyAlgorithmValidator.isValidCombination(rootCertConfig.getKeyAlg(), rootCertConfig.getSignAlg());

        if(!isValid){
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("keyAlg", rootCertConfig.getKeyAlg());
            errorDetails.put("signAlg", rootCertConfig.getSignAlg());
            throw new InvalidKeyAlgorithmCombinationException(
                    "Invalid combination: key algorithm and signing algorithm for Root CA",
                    errorDetails
                    );
        }

        isValid = keyAlgorithmValidator.isValidCombination(intCertConfig.getKeyAlg(), intCertConfig.getSignAlg());

        if(!isValid){
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("keyAlg", intCertConfig.getKeyAlg());
            errorDetails.put("signAlg", intCertConfig.getSignAlg());
            throw new InvalidKeyAlgorithmCombinationException(
                    "Invalid combination: key algorithm and signing algorithm for intermediate CA",
                    errorDetails
            );
        }

        // Full rotation logic with dual intermediate + root + cross-signing
        // 1. Create new root (STANDBY)
        RootCA newRoot = rootService.createRootCA(req.getNewRootAlias(), req.getRootValidDays(), rootCertConfig);


        // 2. Create new intermediate (STANDBY)
        IntermediateCA newInt = intermediateService.createIntermediateCA(req.getNewIntermediateAlias(), newRoot.getId(), req.getIntermediateValidDays(), req.getSans(), intCertConfig);

        // 3. Cross-sign old intermediates with new root for leaf continuity
        intermediateService.crossSignOldIntermediates(newRoot.getId(), newInt.getId());

        // 4. Activate new root + intermediate
        rootService.activateRootCA(newRoot.getId());
        intermediateService.activateIntermediateCA(newInt.getId());

        return ResponseEntity.ok("Root and Intermediate CAs rotated successfully. Zero-downtime leaf continuity ensured.");
    }


    @GetMapping("/status")
    public ResponseEntity<CAStatusResponse> getPkiCaStatus() throws Exception {
        CAStatusResponse statusResponse = intermediateService.getPkiChangeStatus();
        return ResponseEntity.ok(statusResponse);
    }


}

