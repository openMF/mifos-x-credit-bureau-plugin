package org.mifos.creditbureau.cb_ild.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for EncryptionService.
 *
 * Test 1: encrypt then decrypt — roundtrip returns original plaintext
 * Test 2: encrypt called twice — different ciphertext each time (random IV)
 * Test 3: blank key — IllegalStateException at construction
 * Test 4: empty key — IllegalStateException at construction
 * Test 5: special characters — roundtrip correct
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    private static final String TEST_KEY =
            "test-key-32-bytes-long-for-aes!!";

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService(TEST_KEY);
    }

    @Test
    @DisplayName("encrypt then decrypt — returns original plaintext")
    void encryptDecrypt_roundtrip_returnsOriginalPlaintext() {
        String plaintext = "RFC-ABCD123456";

        String ciphertext = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
        assertThat(ciphertext).isNotEqualTo(plaintext);
    }

    @Test
    @DisplayName("encrypt called twice — produces different ciphertext each time")
    void encrypt_calledTwice_producesDifferentCiphertext() {
        String plaintext = "same-plaintext";

        String cipher1 = encryptionService.encrypt(plaintext);
        String cipher2 = encryptionService.encrypt(plaintext);

        assertThat(cipher1).isNotEqualTo(cipher2);
        assertThat(encryptionService.decrypt(cipher1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(cipher2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("blank encryption key — IllegalStateException thrown at construction")
    void constructor_blankKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new EncryptionService("   "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CB_ENC_KEY not set");
    }

    @Test
    @DisplayName("empty encryption key — IllegalStateException thrown at construction")
    void constructor_emptyKey_throwsIllegalStateException() {
        assertThatThrownBy(() -> new EncryptionService(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CB_ENC_KEY not set");
    }

    @Test
    @DisplayName("special characters — encrypt/decrypt roundtrip correct")
    void encryptDecrypt_specialCharacters_roundtripCorrect() {
        String plaintext = "RFC: ABCD-1234 / DOB: 1990-05-15 @ México";

        String ciphertext = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("key not 32 bytes — still works with zero-padding, logs warning")
    void constructor_keyNot32Bytes_worksWithPadding() {
        // Short key is zero-padded with a warning — no exception thrown
        // Production deploy checklist enforces 32 bytes
        EncryptionService svc = new EncryptionService("short-key");
        String plaintext = "test";
        assertThat(svc.decrypt(svc.encrypt(plaintext))).isEqualTo(plaintext);
    }
}
