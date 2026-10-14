package fr.m2.groupe7.kiestla.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordGenerator.
 */
class PasswordGeneratorTest {

    private final PasswordGenerator passwordGenerator = new PasswordGenerator();

    @Test
    void generate_hasExpectedLength() {
        assertEquals(PasswordGenerator.LENGTH, passwordGenerator.generate().length());
    }

    @Test
    void generate_onlyUsesUnambiguousAlphanumericCharacters() {
        for (int i = 0; i < 100; i++) {
            String password = passwordGenerator.generate();
            assertTrue(password.matches("[A-Za-z2-9]+"), password);
            assertFalse(password.matches(".*[0O1lI].*"), password);
        }
    }

    @Test
    void generate_isRandom() {
        assertNotEquals(passwordGenerator.generate(), passwordGenerator.generate());
    }
}
