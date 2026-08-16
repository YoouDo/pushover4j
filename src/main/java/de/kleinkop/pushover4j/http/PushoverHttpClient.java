package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.JsonMapper;
import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.PushoverClient;
import de.kleinkop.pushover4j.PushoverException;
import de.kleinkop.pushover4j.PushoverResponse;

import de.kleinkop.pushover4j.ReceiptResponse;
import de.kleinkop.pushover4j.SoundResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPOutputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class PushoverHttpClient implements PushoverClient {
    private static final Logger logger = LoggerFactory.getLogger(PushoverHttpClient.class);

    private static final Long HTTP_TIMEOUT_IN_SECONDS = 30L;
    private static final int RETRY_ATTEMPTS = 5;
    private static final long DEFAULT_RETRY_INTERVAL = 5000L;
    private static final int ENCRYPTION_KEY_LENGTH_BYTES = 32;
    private static final String ENCRYPTION_KEY_PATTERN = "[0-9a-fA-F]{64}";
    private static final String INVALID_ENCRYPTION_KEY_MESSAGE = "An encryption key is required and must be a 64-character hexadecimal value";
    private static final int IV_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final static String API_MESSAGE_PATH = "/1/messages.json";
    private final static String API_SOUNDS_PATH = "/1/sounds.json";
    private final static String API_RECEIPT_PATH = "/1/receipts/";
    private final static String API_CANCEL_PATH = "/1/receipts/{RECEIPT_ID}/cancel.json";
    private final static String API_CANCEL_BY_TAG_PATH = "/1/receipts/cancel_by_tag/";

    private final String appToken;
    private final String userToken;

    private String apiHost = "https://api.pushover.net";
    private Long httpTimeout = HTTP_TIMEOUT_IN_SECONDS;

    final private HttpRetry httpRetry;

    public PushoverHttpClient(String appToken, String userToken) {
        this(appToken, userToken, RETRY_ATTEMPTS, DEFAULT_RETRY_INTERVAL);
    }

    public PushoverHttpClient(
        String appToken,
        String userToken,
        int retryAttempts,
        long retryInterval
    ) {
        this.appToken = appToken;
        this.userToken = userToken;

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(HTTP_TIMEOUT_IN_SECONDS))
            .build();

        httpRetry = new HttpRetry(httpClient, retryAttempts, retryInterval);
    }

    public PushoverHttpClient withApiHost(String apiHost) {
        this.apiHost = apiHost;
        return this;
    }

    public PushoverHttpClient withHttpTimeout(Long httpTimeout) {
        this.httpTimeout = httpTimeout;
        return this;
    }

    private HttpRequest.Builder defaultRequest(String url) throws URISyntaxException {
        return HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(httpTimeout))
            .version(HttpClient.Version.HTTP_1_1);
    }

    @Override
    public PushoverResponse sendMessage(Message msg) {
        return sendMessageToServer(msg, Optional.empty());
    }

    @Override
    public PushoverResponse sendEncryptedMessage(Message msg, String encryptionKey) {
        validateEncryptionKey(encryptionKey);
        return sendMessageToServer(msg, Optional.of(encryptionKey));
    }

    private String encrypt(String value, String encryptionKey) throws GeneralSecurityException, IOException {
        final byte[] key = decodeEncryptionKey(encryptionKey);
        final byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);

        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        final byte[] ciphertext = cipher.doFinal(gzip(value));

        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(iv);
        mac.update(ciphertext);
        final byte[] hmac = mac.doFinal();

        final byte[] payload = new byte[iv.length + ciphertext.length + hmac.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
        System.arraycopy(hmac, 0, payload, iv.length + ciphertext.length, hmac.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private byte[] decodeEncryptionKey(String encryptionKey) {
        validateEncryptionKey(encryptionKey);

        final byte[] key = new byte[ENCRYPTION_KEY_LENGTH_BYTES];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) Integer.parseInt(encryptionKey.substring(i * 2, i * 2 + 2), 16);
        }
        return key;
    }

    private void validateEncryptionKey(String encryptionKey) {
        if (encryptionKey == null || !encryptionKey.matches(ENCRYPTION_KEY_PATTERN)) {
            throw new PushoverException(INVALID_ENCRYPTION_KEY_MESSAGE);
        }
    }

    private byte[] gzip(String value) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }

    private String maybeEncryptedValue(String value, Optional<String> encryptionKey) throws GeneralSecurityException, IOException {
        if (value == null || encryptionKey.isEmpty()) {
            return value;
        }
        return encrypt(value, encryptionKey.get());
    }

    private PushoverResponse sendMessageToServer(Message msg, Optional<String> encryptionKey) {
        try {
            final MultipartFormData bodyData = new HttpClientMultipartFormBody()
                .plus("token", this.appToken)
                .plus("user", this.userToken)
                .plus("message", maybeEncryptedValue(msg.getMessage(), encryptionKey))
                .plusIfSet("priority", msg.getPriority().getValue())
                .plusIfSet("title", maybeEncryptedValue(msg.getTitle(), encryptionKey))
                .plusIfSet("url", maybeEncryptedValue(msg.getUrl(), encryptionKey))
                .plusIfSet("url_title", maybeEncryptedValue(msg.getUrlTitle(), encryptionKey))
                .plusIfSet("html", msg.isHtml(), "1")
                .plusIfSet("monospace", msg.isMonospace(), "1")
                .plusIfSet("sound", msg.getSound())
                .plusIfSet(
                    "timestamp",
                    msg.getTimestamp() != null,
                    () -> String.valueOf(msg.getTimestamp().toEpochSecond())
                )
                .plusIfSet("device", msg.getDevices())
                .plusIfSet("retry", msg.getRetry())
                .plusIfSet("expire", msg.getExpire())
                .plusIfSet("tags", msg.getTags())
                .plusImageIfSet("attachment", msg.getImage())
                .plusIfSet("ttl", msg.getTtl())
                .plusIfSet("encrypted", encryptionKey.isPresent(), () -> "1")
                .build(UUID.randomUUID().toString());

            final HttpRequest request = defaultRequest(apiHost + API_MESSAGE_PATH)
                .header("Content-Type", bodyData.boundary())
                .POST(
                    HttpRequest.BodyPublishers.ofByteArrays(bodyData.byteArrays())
                )
                .build();

            final HttpResponse<String> response = httpRequest(request);
            logger.info("Response: {}", response.body());
            return JsonMapper.fromJson(response.body(), RawPushoverResponse.class).toDomain(response.headers());
        } catch (Exception e) {
            logger.warn("Error while sending request", e);
            throw new PushoverException("Sending message to Pushover failed", e);
        }
    }

    @Override
    public SoundResponse getSounds() {
        try {
            final HttpRequest request = defaultRequest(apiSoundsUrl())
                .GET()
                .build();

            final HttpResponse<String> response = httpRequest(request);
            return JsonMapper.fromJson(response.body(), RawSoundResponse.class).toDomain();
        } catch (Exception e) {
            throw new PushoverException("Sounds request failed", e);
        }
    }

    @Override
    public ReceiptResponse getEmergencyState(String receiptId) {
        final HttpRequest request;
        try {
            request = defaultRequest(apiReceiptUrl(receiptId))
                .GET()
                .build();
            final HttpResponse<String> response = httpRequest(request);
            return JsonMapper.fromJson(response.body(), RawReceiptResponse.class).toDomain();
        } catch (Exception e) {
            throw new PushoverException("Emergency state request failed", e);
        }
    }

    @Override
    public PushoverResponse cancelEmergencyMessage(String receiptId) {
        try {
            final HttpRequest request = defaultRequest(apiCancelUrl(receiptId))
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new BodyToken(appToken).toString()
                    )
                ).build();
            HttpResponse<String> response = httpRequest(request);
            return JsonMapper.fromJson(response.body(), RawPushoverResponse.class).toDomain(response.headers());
        } catch (Exception e) {
            throw new PushoverException("Cancel emergency message request failed", e);
        }
    }

    @Override
    public PushoverResponse cancelEmergencyMessageByTag(String tag) {
        try {
            HttpRequest request = defaultRequest(apiCancelByTagUrl(tag))
                .timeout(Duration.ofSeconds(15))
                .version(HttpClient.Version.HTTP_2)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new BodyToken(appToken).toString()
                    )
                )
                .build();
            final HttpResponse<String> response = httpRequest(request);
            return JsonMapper.fromJson(response.body(), RawPushoverResponse.class).toDomain(response.headers());
        } catch (Exception e) {
            throw new PushoverException("Cancel emergency message by tag request failed", e);
        }
    }

    private HttpResponse<String> httpRequest(HttpRequest request) {
        try {
            return httpRetry.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (CompletionException completionException) {
            if (completionException.getCause() instanceof RetryException re) {
                throw new RuntimeException(re.getMessage(), re);
            }
            throw new PushoverException("Call to Pushover API failed", completionException.getCause());
        } catch (Exception e) {
            throw new PushoverException("Call to Pushover API failed", e);
        }
    }

    private String apiSoundsUrl() {
        return apiHost + API_SOUNDS_PATH + "?token=" + appToken;
    }

    private String apiReceiptUrl(String receiptId) {
        return apiHost + API_RECEIPT_PATH + receiptId + ".json?token=" + appToken;
    }

    private String apiCancelUrl(String receiptId) {
        return apiHost + API_CANCEL_PATH.replace("{RECEIPT_ID}", receiptId);
    }

    private String apiCancelByTagUrl(String tag) {
        return apiHost + API_CANCEL_BY_TAG_PATH + tag + ".json";
    }
}
