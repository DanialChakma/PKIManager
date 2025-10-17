package com.pki.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PasswordUtil {

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "@#$_-!%&*?";
    private static final String ALL = UPPER + LOWER + DIGITS + SPECIAL;
    private static final SecureRandom RANDOM = createSecureRandom();

    private static SecureRandom createSecureRandom() {
        try {
            return SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            return new SecureRandom();
        }
    }

    public static String generateStrongPassword() {
        int length = 12;

        // Functional, loop-free composition
        List<Character> chars = Stream.concat(
                // ensure at least one of each category
                Stream.of(randomChar(UPPER), randomChar(LOWER), randomChar(DIGITS), randomChar(SPECIAL)),
                // fill remaining with random chars
                RANDOM.ints(length - 4, 0, ALL.length())
                        .mapToObj(ALL::charAt)
        ).collect(Collectors.toList());

        // Shuffle + rotate to enhance entropy distribution
        Collections.shuffle(chars, RANDOM);
        Collections.rotate(chars, RANDOM.nextInt(length));

        // Return as String
        return chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private static char randomChar(String chars) {
        return chars.charAt(RANDOM.nextInt(chars.length()));
    }
}
