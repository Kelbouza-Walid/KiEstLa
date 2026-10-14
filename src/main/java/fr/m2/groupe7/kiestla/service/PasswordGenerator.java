package fr.m2.groupe7.kiestla.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates random initial passwords for newly created users.
 */
@Component
public class PasswordGenerator {

    public static final int LENGTH = 12;

    // Letters and digits, without the ambiguous ones (0/O, 1/l/I)
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }
}
