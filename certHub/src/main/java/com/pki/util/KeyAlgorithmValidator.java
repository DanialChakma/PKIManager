package com.pki.util;

import com.pki.exception.InvalidKeyAlgorithmCombinationException;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KeyAlgorithmValidator {

    private final Map<String, Set<String>> validCombinations = new HashMap<>();

    private static final Set<String> NO_SIGNATURE_KEY_TYPES = new HashSet<>(
            Arrays.asList("X25519", "X448", "DH")
    );

    public KeyAlgorithmValidator() {
        // Normalize to uppercase to avoid case issues
        validCombinations.put("RSA", new HashSet<>(Arrays.asList(
                "SHA1withRSA",
                "SHA256withRSA",
                "SHA384withRSA",
                "SHA512withRSA",
                "SHA3-256withRSA",
                "SHA256withRSAandMGF1" // RSA-PSS
        )));

        validCombinations.put("DSA", new HashSet<>(Arrays.asList(
                "SHA1withDSA",
                "SHA256withDSA",
                "SHA384withDSA",
                "SHA512withDSA"
        )));

        validCombinations.put("EC", new HashSet<>(Arrays.asList(
                "SHA256withECDSA",
                "SHA384withECDSA",
                "SHA512withECDSA",
                "SHA3-256withECDSA"
        )));

        validCombinations.put("ED25519", Collections.singleton("Ed25519"));
        validCombinations.put("ED448", Collections.singleton("Ed448"));

        validCombinations.put("X25519", Collections.<String>emptySet());
        validCombinations.put("X448", Collections.<String>emptySet());
        validCombinations.put("DH", Collections.<String>emptySet());
    }

    public void validateCombination(String keyType, String signatureAlgorithm) {
        if (keyType == null || signatureAlgorithm == null) {
            throw new InvalidKeyAlgorithmCombinationException("Key type and signature algorithm must not be null.");
        }

        String normalizedKeyType = keyType.toUpperCase(Locale.ROOT);
        String normalizedSigAlg = signatureAlgorithm.trim();

        Set<String> allowedAlgorithms = validCombinations.get(normalizedKeyType);
        if (allowedAlgorithms == null || !allowedAlgorithms.contains(normalizedSigAlg)) {
            throw new InvalidKeyAlgorithmCombinationException(
                    "Invalid combination: KeyType '" + keyType + "' cannot be used with SignatureAlgorithm '" + signatureAlgorithm + "'"
            );
        }
    }


    public boolean isValidCombination(String keyType, String signatureAlgorithm) {
        if (keyType == null ) {
            return false;
        }

        String normalizedKeyType = keyType.toUpperCase(Locale.ROOT);
        String normalizedSigAlg = signatureAlgorithm != null ? signatureAlgorithm.trim(): "";

        if (NO_SIGNATURE_KEY_TYPES.contains(normalizedKeyType)) {
            return normalizedSigAlg.isEmpty();
        }

        Set<String> allowedAlgorithms = validCombinations.get(normalizedKeyType);
        return allowedAlgorithms != null && allowedAlgorithms.contains(normalizedSigAlg);
    }

}

