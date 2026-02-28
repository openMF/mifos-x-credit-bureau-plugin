package org.mifos.creditbureau.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EncryptionService}.
 * <p>
 * Verifies AES-256-GCM encryption and decryption behaviour including:
 * round-trip correctness, IV randomness, tamper detection, and edge cases.
 */
class EncryptionServiceUnitTest {

    private EncryptionService encryptionService;

    // Base64-encoded 32-byte AES-256 key (matches test application.properties)
    private static final String TEST_KEY = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0NTY=";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_KEY);
    }

    @Test
    @DisplayName("encrypt() should return a non-null, non-empty Base64 string")
    void encryptReturnsNonNullBase64String() throws Exception {
        String plaintext = "my-secret-api-key";

        String encrypted = encryptionService.encrypt(plaintext);

        assertNotNull(encrypted, "Encrypted value should not be null");
        assertFalse(encrypted.isEmpty(), "Encrypted value should not be empty");
        // Should not throw when decoded as Base64
        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(encrypted),
                "Encrypted value should be valid Base64");
    }

    @Test
    @DisplayName("decrypt(encrypt(plaintext)) should return the original plaintext")
    void decryptReturnsOriginalPlaintext() throws Exception {
        String plaintext = "super-secret-password-123!";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted,
                "Decrypted value should match the original plaintext");
    }

    @Test
    @DisplayName("encrypt() should produce different ciphertext each time due to random IV")
    void encryptProducesDifferentCiphertextEachTime() throws Exception {
        String plaintext = "same-secret-value";

        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2,
                "Two encryptions of the same plaintext should produce different ciphertexts (random IV)");

        // But both should decrypt to the same value
        assertEquals(encryptionService.decrypt(encrypted1),
                encryptionService.decrypt(encrypted2),
                "Both ciphertexts should decrypt to the same original plaintext");
    }

    @Test
    @DisplayName("decrypt() should throw an exception when ciphertext is tampered with")
    void decryptWithTamperedCiphertextThrowsException() throws Exception {
        String plaintext = "sensitive-data";
        String encrypted = encryptionService.encrypt(plaintext);

        // Tamper with the ciphertext by flipping a character in the middle
        char[] chars = encrypted.toCharArray();
        int midpoint = chars.length / 2;
        chars[midpoint] = (chars[midpoint] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);

        assertThrows(Exception.class,
                () -> encryptionService.decrypt(tampered),
                "Decrypting tampered ciphertext should throw an exception (GCM authentication failure)");
    }

    @Test
    @DisplayName("encrypt/decrypt should handle special characters and Unicode correctly")
    void encryptAndDecryptWithSpecialCharacters() throws Exception {
        String plaintext = "pässwörd!@#$%^&*()_+-={}[]|\\:\";<>?,./~` 你好世界 🔐";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted,
                "Special characters and Unicode should survive encrypt/decrypt round-trip");
    }

    @Test
    @DisplayName("encrypt/decrypt should handle an empty string")
    void encryptAndDecryptEmptyString() throws Exception {
        String plaintext = "";

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted,
                "Empty string should survive encrypt/decrypt round-trip");
    }

    @Test
    @DisplayName("encrypt/decrypt should handle a long string (simulating large API keys)")
    void encryptAndDecryptLongString() throws Exception {
        // Build a long string simulating a large API key or certificate
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("LongApiKeySegment-").append(i).append("-");
        }
        String plaintext = sb.toString();

        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        assertEquals(plaintext, decrypted,
                "Long strings should survive encrypt/decrypt round-trip");
    }
}
