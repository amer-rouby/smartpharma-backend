package com.smartpharma.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    /**
     * RFC 6238 Appendix B publishes 8-digit reference codes for a fixed ASCII secret
     * at specific Unix times. This app uses 6 digits, but the underlying HOTP integer
     * (HMAC-SHA1 + dynamic truncation) is identical either way - only the final
     * "mod 10^digits" step differs - so the last 6 digits of each published 8-digit
     * vector must equal what this implementation produces at the same counter.
     *
     * This calls the package-private generateCode(key, counter) directly rather than
     * the public verifyCode(), because verifyCode() reads the real system clock
     * internally (by design - it's a small, dependency-free class with no injectable
     * Clock) and these vectors are fixed historical timestamps from decades ago that
     * the real clock will never match. generateCode() is deterministic given a
     * counter, so it's the right seam for reproducing a standard's published vectors.
     */
    @ParameterizedTest
    @CsvSource({
            "59, 94287082",
            "1111111109, 07081804",
            "1111111111, 14050471",
            "1234567890, 89005924",
            "2000000000, 69279037"
    })
    void generateCode_matchesRfc6238ReferenceVectors(long unixTimeSeconds, String expected8Digit) throws Exception {
        String expected6Digit = expected8Digit.substring(2);
        byte[] key = "12345678901234567890".getBytes("US-ASCII");
        long counter = unixTimeSeconds / 30;

        Method generateCode = TotpService.class.getDeclaredMethod("generateCode", byte[].class, long.class);
        generateCode.setAccessible(true);
        String actual = (String) generateCode.invoke(totpService, key, counter);

        assertThat(actual).isEqualTo(expected6Digit);
    }

    @Test
    void generateSecret_producesDifferentSecretsEachTime() {
        String first = totpService.generateSecret();
        String second = totpService.generateSecret();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).matches("[A-Z2-7]+"); // valid Base32 alphabet
    }

    @Test
    void verifyCode_roundTripsWithGeneratedSecret() {
        String secret = totpService.generateSecret();
        String currentCode = computeCurrentCode(secret);

        assertThat(totpService.verifyCode(secret, currentCode)).isTrue();
    }

    @Test
    void verifyCode_rejectsWrongCode() {
        String secret = totpService.generateSecret();
        String currentCode = computeCurrentCode(secret);
        // Flip the code to something that's overwhelmingly unlikely to also be valid
        // (a real code, or an adjacent-window code, would otherwise make this flaky).
        String wrongCode = currentCode.equals("000000") ? "111111" : "000000";

        assertThat(totpService.verifyCode(secret, wrongCode)).isFalse();
    }

    @Test
    void verifyCode_rejectsMalformedInput() {
        String secret = totpService.generateSecret();

        assertThat(totpService.verifyCode(secret, null)).isFalse();
        assertThat(totpService.verifyCode(secret, "12345")).isFalse(); // too short
        assertThat(totpService.verifyCode(secret, "1234567")).isFalse(); // too long
        assertThat(totpService.verifyCode(secret, "abcdef")).isFalse(); // not digits
    }

    @Test
    void verifyCode_toleratesOneStepClockDrift() throws Exception {
        String secret = totpService.generateSecret();

        Method decode = TotpService.class.getDeclaredMethod("base32Decode", String.class);
        Method generateCode = TotpService.class.getDeclaredMethod("generateCode", byte[].class, long.class);
        decode.setAccessible(true);
        generateCode.setAccessible(true);
        byte[] key = (byte[]) decode.invoke(totpService, secret);
        long currentCounter = System.currentTimeMillis() / 1000L / 30L;

        String previousStepCode = (String) generateCode.invoke(totpService, key, currentCounter - 1);
        String nextStepCode = (String) generateCode.invoke(totpService, key, currentCounter + 1);
        String twoStepsAwayCode = (String) generateCode.invoke(totpService, key, currentCounter + 2);

        assertThat(totpService.verifyCode(secret, previousStepCode)).isTrue();
        assertThat(totpService.verifyCode(secret, nextStepCode)).isTrue();
        assertThat(totpService.verifyCode(secret, twoStepsAwayCode)).isFalse();
    }

    @Test
    void buildOtpAuthUrl_containsExpectedStandardFields() {
        String secret = totpService.generateSecret();
        String url = totpService.buildOtpAuthUrl(secret, "admin", "SmartPharma");

        assertThat(url).startsWith("otpauth://totp/");
        assertThat(url).contains("secret=" + secret);
        assertThat(url).contains("issuer=SmartPharma");
        assertThat(url).contains("algorithm=SHA1");
        assertThat(url).contains("digits=6");
        assertThat(url).contains("period=30");
    }

    private String computeCurrentCode(String base32Secret) {
        try {
            Method decode = TotpService.class.getDeclaredMethod("base32Decode", String.class);
            Method generate = TotpService.class.getDeclaredMethod("generateCode", byte[].class, long.class);
            decode.setAccessible(true);
            generate.setAccessible(true);
            byte[] key = (byte[]) decode.invoke(totpService, base32Secret);
            long counter = System.currentTimeMillis() / 1000L / 30L;
            return (String) generate.invoke(totpService, key, counter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
