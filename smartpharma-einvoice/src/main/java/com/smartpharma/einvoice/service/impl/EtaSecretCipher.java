package com.smartpharma.einvoice.service.impl;

import com.smartpharma.common.exception.LocalizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

// AES-256-GCM for the per-pharmacy ETA secrets (client secret, POS pre-shared
// key) stored in the database. The key comes from ETA_CREDENTIALS_KEY
// (base64 of 32 random bytes) and is never stored with the data. Without it,
// secrets can't be saved or read - there is deliberately no plaintext fallback.
@Component
public class EtaSecretCipher {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec key;

    public EtaSecretCipher(@Value("${eta.credentials-key:}") String base64Key) {
        this.key = parseKey(base64Key);
    }

    public boolean isConfigured() {
        return key != null;
    }

    public String encrypt(String plaintext) {
        requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt ETA secret", e);
        }
    }

    public String decrypt(String stored) {
        requireKey();
        try {
            byte[] data = Base64.getDecoder().decode(stored);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            return new String(cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new LocalizedException(HttpStatus.INTERNAL_SERVER_ERROR, "ETA_SECRET_UNREADABLE",
                    "Stored ETA secret can't be decrypted - was ETA_CREDENTIALS_KEY changed?", e);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new LocalizedException(HttpStatus.SERVICE_UNAVAILABLE, "ETA_CREDENTIALS_KEY_MISSING",
                    "ETA_CREDENTIALS_KEY is not configured on the server");
        }
    }

    private static SecretKeySpec parseKey(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        byte[] raw = Base64.getDecoder().decode(base64Key.trim());
        if (raw.length != 32) {
            throw new IllegalStateException("ETA_CREDENTIALS_KEY must be base64 of exactly 32 bytes, got " + raw.length);
        }
        return new SecretKeySpec(raw, "AES");
    }
}
