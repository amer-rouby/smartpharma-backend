package com.smartpharma.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * RFC 6238 (TOTP) on top of RFC 4226 (HOTP), HMAC-SHA1 / 6 digits / 30s step - the
 * same parameters Google Authenticator, Authy, etc. all assume by default, so any
 * standard authenticator app works without extra configuration. No external
 * dependency: HMAC-SHA1 is built into the JDK (javax.crypto.Mac), and the only other
 * piece is Base32 encode/decode for the human-typeable secret, which is short enough
 * to just implement here rather than pull in a library for.
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20; // 160 bits, the RFC-recommended HOTP key size
    private static final int DIGITS = 6;
    private static final int STEP_SECONDS = 30;
    private static final int ALLOWED_DRIFT_STEPS = 1; // accept the previous/current/next 30s window
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final SecureRandom secureRandom = new SecureRandom();

    /** Generates a fresh random secret, Base32-encoded for display/manual entry. */
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Standard otpauth:// URI that authenticator apps scan (as a QR code, rendered
     * client-side - this backend never generates an image).
     */
    public String buildOtpAuthUrl(String base32Secret, String accountName, String issuer) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        return String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedIssuer, encodedAccount, base32Secret, encodedIssuer, DIGITS, STEP_SECONDS);
    }

    /** Verifies a user-entered code against the secret, tolerating small clock drift. */
    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || !code.matches("\\d{6}")) {
            return false;
        }
        long currentStep = System.currentTimeMillis() / 1000L / STEP_SECONDS;
        byte[] key = base32Decode(base32Secret);
        for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            String candidate = generateCode(key, currentStep + drift);
            if (candidate.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generateCode(byte[] key, long counter) {
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counterBytes[i] = (byte) (counter & 0xFF);
            counter >>= 8;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac = mac.doFinal(counterBytes);

            int offset = hmac[hmac.length - 1] & 0x0F;
            int binary = ((hmac[offset] & 0x7F) << 24)
                    | ((hmac[offset + 1] & 0xFF) << 16)
                    | ((hmac[offset + 2] & 0xFF) << 8)
                    | (hmac[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder result = new StringBuilder();
        int bits = 0;
        int value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                result.append(BASE32_ALPHABET.charAt((value >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            result.append(BASE32_ALPHABET.charAt((value << (5 - bits)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] base32Decode(String encoded) {
        String sanitized = encoded.trim().toUpperCase(Locale.ROOT).replace("=", "");
        int bits = 0;
        int value = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : sanitized.toCharArray()) {
            int idx = BASE32_ALPHABET.indexOf(c);
            if (idx < 0) {
                continue;
            }
            value = (value << 5) | idx;
            bits += 5;
            if (bits >= 8) {
                out.write((value >> (bits - 8)) & 0xFF);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }
}
