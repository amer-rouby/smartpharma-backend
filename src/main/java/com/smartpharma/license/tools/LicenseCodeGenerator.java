package com.smartpharma.license.tools;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

// Vendor-only tool - run this locally whenever a pharmacy pays for another
// month, NEVER deploy it (and never deploy the private key file it reads) to
// a customer's machine. It just prints a renewal code to paste into the app's
// "Renew subscription" screen. Plain main(), no Spring context needed:
//
//   mvn compile exec:java -Dexec.mainClass=com.smartpharma.license.tools.LicenseCodeGenerator \
//       -Dexec.args="E:/Backend/Projects/smartpharma-license-keys/license-private.pem 1 1"
//
// args: <private-key-pem-path> <pharmacyId> <months>
public final class LicenseCodeGenerator {

    private LicenseCodeGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: LicenseCodeGenerator <private-key-pem-path> <pharmacyId> <months>");
            System.exit(1);
        }

        Path privateKeyPath = Path.of(args[0]);
        Long pharmacyId = Long.valueOf(args[1]);
        int months = Integer.parseInt(args[2]);

        PrivateKey privateKey = loadPrivateKey(privateKeyPath);
        Date expiresAt = Date.from(addMonths(Instant.now(), months));

        String code = Jwts.builder()
                .claim("pharmacyId", pharmacyId)
                .setIssuedAt(new Date())
                .setExpiration(expiresAt)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        System.out.println("Pharmacy ID: " + pharmacyId);
        System.out.println("Expires at:  " + expiresAt.toInstant().atZone(ZoneId.systemDefault()));
        System.out.println();
        System.out.println("Renewal code (send this to the customer):");
        System.out.println(code);
    }

    private static Instant addMonths(Instant now, int months) {
        return now.atZone(ZoneId.systemDefault()).plusMonths(months).toInstant();
    }

    private static PrivateKey loadPrivateKey(Path pemPath) throws IOException, GeneralSecurityException {
        String pem = Files.readString(pemPath, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }
}
