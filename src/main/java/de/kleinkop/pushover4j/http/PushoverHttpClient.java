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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public class PushoverHttpClient implements PushoverClient {
    private static final Logger logger = LoggerFactory.getLogger(PushoverHttpClient.class);

    private static final Long HTTP_TIMEOUT_IN_SECONDS = 30L;
    private static final int RETRY_ATTEMPTS = 5;
    private static final long DEFAULT_RETRY_INTERVAL = 5000L;

    private final String appToken;
    private final String userToken;

    private PushoverApiPaths pushoverApiPaths = PushoverApiPaths.defaults();

    private Long httpTimeout = HTTP_TIMEOUT_IN_SECONDS;

    private final HttpRetry httpRetry;

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

        this.httpRetry = new HttpRetry(httpClient, retryAttempts, retryInterval);
    }

    public PushoverHttpClient withApiPaths(PushoverApiPaths apiPaths) {
        this.pushoverApiPaths = apiPaths;
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
        return sendMessageToServer(msg, null);
    }

    @Override
    public PushoverResponse sendEncryptedMessage(Message msg, String encryptionKey) {
        PushoverEncryption.validateEncryptionKey(encryptionKey);
        return sendMessageToServer(msg, encryptionKey);
    }

    private String maybeEncryptedValue(String value, String encryptionKey) {
        if (value == null || encryptionKey == null) {
            return value;
        }
        return PushoverEncryption.encrypt(value, encryptionKey);
    }

    private PushoverResponse sendMessageToServer(Message msg, String encryptionKey) {
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
                .plusIfSet("encrypted", encryptionKey != null, () -> "1")
                .build(UUID.randomUUID().toString());

            final HttpRequest request = defaultRequest(pushoverApiPaths.messagePath())
                .header("Content-Type", bodyData.boundary())
                .POST(
                    HttpRequest.BodyPublishers.ofByteArrays(bodyData.byteArrays())
                )
                .build();

            final HttpResponse<String> response = httpRequest(request);
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
        return pushoverApiPaths.soundsPath() + "?token=" + appToken;
    }

    private String apiReceiptUrl(String receiptId) {
        return pushoverApiPaths.receiptPath() + receiptId + ".json?token=" + appToken;
    }

    private String apiCancelUrl(String receiptId) {
        return pushoverApiPaths.cancelPath().replace("{RECEIPT_ID}", receiptId);
    }

    private String apiCancelByTagUrl(String tag) {
        return pushoverApiPaths.cancelByTagPath() + tag + ".json";
    }
}
