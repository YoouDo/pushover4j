package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.PushoverException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;

/**
 * Pushover end-to-end encryption (<a href="https://pushover.net/api">See Pushover</a>, "End-to-End Encryption").
 * <p>
 * Scheme defined by Pushover:
 *   1. GZIP compress the plaintext
 *   2. random 16-byte IV
 *   3. AES-256-CBC, PKCS7 padding (applied explicitly; the JDK cipher runs with NoPadding)
 *   4. HMAC-SHA256 over (IV || ciphertext), using the same 256-bit key
 *   5. Base64( IV || ciphertext || HMAC )
 * <p>
 * Applies to the message, title, url and url_title fields; request carries encrypted=1.
 * Key: 256-bit, configured in the Pushover app as a 64-character hex string.
 * <p>
 * Java 21, JDK only.
 */
public final class PushoverEncryption {
    private static final int KEY_LEN = 32;
    private static final int IV_LEN = 16;
    private static final int MAC_LEN = 32;
    private static final int BLOCK = 16;
    private static final String ENCRYPTION_KEY_PATTERN = "[0-9a-fA-F]{64}";
    private static final String INVALID_ENCRYPTION_KEY_MESSAGE = "An encryption key is required and must be a 64-character hexadecimal value";

    private static final SecureRandom RNG = new SecureRandom();

    private PushoverEncryption() {}

    private static byte[] parseKey(String hex) {
        if (hex == null || hex.length() != KEY_LEN * 2) {
            throw new IllegalArgumentException("Key must be exactly 64 hex characters (256 bit)");
        }
        try {
            return HexFormat.of().parseHex(hex);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Key must be hexadecimal"); // don't echo key material
        }
    }

    /** Encrypts a field value per the Pushover scheme; returns the Base64 string to send. */
    public static String encrypt(String plaintext, String keyHex) {
        byte[] key = parseKey(keyHex);
        byte[] compressed = null;
        byte[] padded = null;
        try {
            compressed = gzip(plaintext.getBytes(StandardCharsets.UTF_8));
            padded = pkcs7Pad(compressed);

            byte[] iv = new byte[IV_LEN];
            RNG.nextBytes(iv); // fresh random IV for every field of every message

            // CBC is mandated by the Pushover spec. JDK padding is disabled and PKCS7 is applied
            // explicitly (see pkcs7Pad/pkcs7Unpad); integrity is enforced by encrypt-then-MAC.
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] ct = cipher.doFinal(padded);

            byte[] out = new byte[IV_LEN + ct.length + MAC_LEN];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update(out, 0, IV_LEN + ct.length);          // encrypt-then-MAC
            mac.doFinal(out, IV_LEN + ct.length);

            return Base64.getEncoder().encodeToString(out);   // standard Base64 with padding
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Encryption failed", e);
        } finally {
            Arrays.fill(key, (byte) 0);
            if (compressed != null) Arrays.fill(compressed, (byte) 0);
            if (padded != null) Arrays.fill(padded, (byte) 0);
        }
    }

    /** PKCS7: append n bytes of value n, where n = 16 - (len mod 16), i.e. always 1..16. */
    private static byte[] pkcs7Pad(byte[] data) {
        int n = BLOCK - (data.length % BLOCK);
        byte[] out = Arrays.copyOf(data, data.length + n);
        Arrays.fill(out, data.length, out.length, (byte) n);
        return out;
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(bos)) {
            gz.write(data);
        }
        return bos.toByteArray();
    }

    public static void validateEncryptionKey(String encryptionKey) {
        if (encryptionKey == null || !encryptionKey.matches(ENCRYPTION_KEY_PATTERN)) {
            throw new PushoverException(INVALID_ENCRYPTION_KEY_MESSAGE);
        }
    }
}
