package de.kleinkop.pushover4j;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import de.kleinkop.pushover4j.http.PushoverApiPaths;
import de.kleinkop.pushover4j.http.PushoverHttpClient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WireMockTest
public class PushoverClientTest {
    static PushoverClient pushoverClient;
    static PushoverClient invalidClient;

    final LocalDateTime deliveredAt = LocalDateTime.parse("2023-01-07T10:00:00");
    final LocalDateTime expiresAt = LocalDateTime.parse("2023-01-07T11:00:00");

    @BeforeAll
    static void beforeAll(WireMockRuntimeInfo runtimeInfo) {
        PushoverApiPaths apiPaths = PushoverApiPaths.defaultsWithApiHost("http://localhost:" + runtimeInfo.getHttpPort());
        pushoverClient = new PushoverHttpClient(
            "app-token",
            "user-token",
            5,
            10L
        )
            .withApiPaths(apiPaths)
            .withHttpTimeout(10L);

        invalidClient = new PushoverHttpClient(
            "invalid-token",
            null
        ).withApiPaths(apiPaths);
    }

    @Test
    void fetchSoundsTest() {
        var sounds = pushoverClient.getSounds();
        assertEquals(1, sounds.status());
        assertEquals("A", sounds.request());

        sounds = invalidClient.getSounds();
        assertEquals(0, sounds.status());
        assertNull(sounds.sounds());
        assertEquals("invalid", sounds.token());
        assertNotNull(sounds.errors());
        assertEquals(1, sounds.errors().size());
        assertEquals("application token is invalid", sounds.errors().getFirst());
        assertEquals("error-request", sounds.request());
    }

    @Test
    void testInvalidMessages() {
        final Message.Builder emergencyMsgWithoutRetryAndExpiration =
            Message
                .of("msg")
                .withPriority(Priority.EMERGENCY);

        assertThrows(
            IllegalArgumentException.class,
            emergencyMsgWithoutRetryAndExpiration::build
        );

        // emergency msg without expiration
        final Message.Builder emergencyMsgWithoutExpiration =
            Message
                .of("msg")
                .withPriority(Priority.EMERGENCY)
                .withRetry(30);

        assertThrows(
            IllegalArgumentException.class,
            emergencyMsgWithoutExpiration::build
        );

        // emergency msg without retry
        final Message.Builder msgWithoutRetry =
            Message
                .of("msg")
                .withPriority(Priority.EMERGENCY)
                .withExpiration(60);

        assertThrows(
            IllegalArgumentException.class,
            msgWithoutRetry::build
        );

        // Message with options html and monospace
        final Message.Builder msgWithHtmlAndMonospace =
            Message
                .of("msg")
                .withHtml(true)
                .withMonospace(true);

        assertThrows(
            IllegalArgumentException.class,
            msgWithHtmlAndMonospace::build
        );
    }

    @SuppressWarnings("UastIncorrectHttpHeaderInspection")
    @Test
    void testSimpleMessage() {
        stubFor(
            post("/1/messages.json")
                .willReturn(
                    okJson(
                        """
                              {
                                  "status": 1,
                                  "request": "B"
                              }
                            """
                    )
                        .withHeader("X-Limit-App-Limit", "10000")
                        .withHeader("X-Limit-App-Remaining", "1234")
                        .withHeader("X-Limit-App-Reset", "1393653600")
                )
        );

        final PushoverResponse response = pushoverClient.sendMessage(
            Message.of("Testing").build()
        );

        assertEquals(1, response.status());
        assertEquals("B", response.request());
        assertNotNull(response.applicationUsage());
        assertEquals(10_000, response.applicationUsage().limit());
        assertEquals(1234, response.applicationUsage().remaining());
        assertEquals(OffsetDateTime.parse("2014-03-01T06:00:00Z"), response.applicationUsage().reset());

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .toList();

        assertEquals(1, loggedRequests.size());
        assertEquals("app-token", partAsString(loggedRequests.getFirst(), "token"));
        assertEquals("user-token", partAsString(loggedRequests.getFirst(), "user"));
        assertEquals("Testing", partAsString(loggedRequests.getFirst(), "message"));
    }

    @Test
    void testEncryptedMessage() throws GeneralSecurityException, IOException {
        stubFor(post("/1/messages.json").willReturn(okJson("""
            {
                "status": 1,
                "request": "encrypted"
            }
            """)));

        final String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        final PushoverResponse response = pushoverClient.sendEncryptedMessage(
            Message.of("Secret message")
                .withTitle("Secret title")
                .withUrl("https://example.test/secret")
                .withUrlTitle("Secret link")
                .withPriority(Priority.HIGH)
                .build(),
            encryptionKey
        );

        assertEquals(1, response.status());
        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .toList();
        final LoggedRequest request = loggedRequests.getLast();

        assertEquals("app-token", partAsString(request, "token"));
        assertEquals("user-token", partAsString(request, "user"));
        assertEquals("1", partAsString(request, "encrypted"));
        assertEquals("1", partAsString(request, "priority"));
        assertEncryptedValue(request, "message", "Secret message", encryptionKey);
        assertEncryptedValue(request, "title", "Secret title", encryptionKey);
        assertEncryptedValue(request, "url", "https://example.test/secret", encryptionKey);
        assertEncryptedValue(request, "url_title", "Secret link", encryptionKey);
        assertNotEquals(partAsString(request, "message"), partAsString(request, "title"));
    }

    @Test
    void testEncryptedMessageOmitsNullOptionalFields() {
        stubFor(post("/1/messages.json").willReturn(okJson("""
            {
                "status": 1,
                "request": "encrypted-minimal"
            }
            """)));

        final String encryptionKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        pushoverClient.sendEncryptedMessage(Message.of("Secret message").build(), encryptionKey);

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .toList();
        final LoggedRequest request = loggedRequests.getLast();
        assertFalse(hasPart(request, "title"));
        assertFalse(hasPart(request, "url"));
        assertFalse(hasPart(request, "url_title"));
    }

    @Test
    void testEncryptedMessageRejectsMissingOrMalformedKeys() {
        final Message secretMessage = Message.of("Secret message").build();
        final PushoverException missingKeyException = assertThrows(
            PushoverException.class,
            () -> pushoverClient.sendEncryptedMessage(secretMessage, null)
        );
        assertEquals(
            "An encryption key is required and must be a 64-character hexadecimal value",
            missingKeyException.getMessage()
        );

        final PushoverException malformedKeyException = assertThrows(
            PushoverException.class,
            () -> pushoverClient.sendEncryptedMessage(secretMessage, "not-a-valid-key")
        );
        assertEquals(
            "An encryption key is required and must be a 64-character hexadecimal value",
            malformedKeyException.getMessage()
        );
        verify(0, postRequestedFor(urlPathEqualTo("/1/messages.json")));
    }

    @Test
    void testEmergencyMessage() {
        stubsForEmergencyMessage();

        final PushoverResponse response = pushoverClient.sendMessage(
            Message.of("Testing emergency")
                .withPriority(Priority.EMERGENCY)
                .withRetry(100)
                .withExpiration(200)
                .withTag("TAG")
                .withTimestamp(OffsetDateTime.now())
                .withImage(new File(TestUtils.getResource("/testdata/image.png").getFile()))
                .build()
        );

        assertEquals(1, response.status());
        assertEquals("C", response.request());
        assertEquals("R", response.receipt());

        final Optional<LoggedRequest> first = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .findFirst();

        assertTrue(first.isPresent());
        var request = first.get();

        assertEquals("100", partAsString(request, "retry"));
        assertEquals("200", partAsString(request, "expire"));
        assertEquals("2", partAsString(request, "priority"));
        assertEquals("TAG", partAsString(request, "tags"));

        ReceiptResponse receiptResponse = pushoverClient.getEmergencyState("R");

        assertEquals(1, receiptResponse.status());
        assertEquals("D", receiptResponse.request());
        assertEquals(deliveredAt, receiptResponse.lastDeliveredAt());
        assertEquals(expiresAt, receiptResponse.expirersAt());
        assertFalse(receiptResponse.acknowledged());
        assertNull(receiptResponse.acknowledgedBy());
        assertNull(receiptResponse.acknowledgedByDevice());
        assertNull(receiptResponse.calledBackAt());

        receiptResponse = pushoverClient.getEmergencyState("R1");

        assertEquals(1, receiptResponse.status());
        assertEquals("D", receiptResponse.request());
        assertEquals(deliveredAt, receiptResponse.lastDeliveredAt());
        assertEquals(expiresAt, receiptResponse.expirersAt());
        assertTrue(receiptResponse.acknowledged());
        assertEquals("user1", receiptResponse.acknowledgedBy());
        assertEquals("device", receiptResponse.acknowledgedByDevice());
        assertNull(receiptResponse.calledBackAt());
    }

    @Test
    @DisplayName("Check exception is thrown after five retries")
    void checkExceptionIsThrownAfterFiveRetries() {
        stubFor(
            post("/1/messages.json")
                .inScenario("Retry2")
                .willReturn(
                    aResponse().withStatus(500).withBody("Unknown server error.")
                )
        );

        final Message testMessage = Message.of("Testing").build();
        final RuntimeException exception = assertThrows(PushoverException.class, () -> pushoverClient.sendMessage(testMessage));
        assertTrue(exception.getMessage().startsWith("Sending message to Pushover failed"));
    }

    @Test
    @DisplayName("Check if five retries are executed")
    void checkRetriesExecuted() {
        stubFor(
            post("/1/messages.json")
                .inScenario("Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(
                    aResponse().withStatus(500)
                )
                .willSetStateTo("STEP-1")
        );

        for (int i = 1; i <= 4; i++) {
            stubFor(
                post("/1/messages.json")
                    .inScenario("Retry")
                    .whenScenarioStateIs("STEP-" + i)
                    .willReturn(
                        aResponse().withStatus(500)
                    )
                    .willSetStateTo("STEP-" + (i + 1))
            );
        }

        stubFor(
            post("/1/messages.json")
                .inScenario("Retry")
                .whenScenarioStateIs("STEP-5")
                .willReturn(
                    okJson(
                        """
                                {
                                    "status": 1,
                                    "request": "B"
                                }
                            """
                    )
                )
        );

        final PushoverResponse response = pushoverClient.sendMessage(Message.of("Testing").build());
        assertEquals(1, response.status());
        assertEquals("B", response.request());

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .toList();

        assertEquals(6, loggedRequests.size());
        var last = loggedRequests.get(5);

        assertEquals("app-token", partAsString(last, "token"));
        assertEquals("user-token", partAsString(last, "user"));
        assertEquals("Testing", partAsString(last, "message"));

        verify(6, postRequestedFor(urlPathEqualTo("/1/messages.json")));
    }

    private void stubsForEmergencyMessage() {
        stubFor(
            post("/1/messages.json")
                .willReturn(
                    okJson(
                        """
                                {
                                    "receipt": "R",
                                    "status": 1,
                                    "request": "C"
                                }
                            """
                    )
                )
        );

        stubFor(
            get("/1/receipts/R.json?token=app-token")
                .willReturn(
                    okJson(
                        """
                            {
                                "status": 1,
                                "acknowledged": 0,
                                "acknowledged_at": 0,
                                "acknowledged_by": "",
                                "acknowledged_by_device": "",
                                "last_delivered_at": %s,
                                "expired": 1,
                                "expires_at": %s,
                                "called_back": 0,
                                "called_back_at": 0,
                                "request": "D"
                            }
                        """.formatted(
                            deliveredAt.toInstant(ZoneOffset.UTC).getEpochSecond(),
                            expiresAt.toInstant(ZoneOffset.UTC).getEpochSecond()
                        )
                    )
                )
        );

        stubFor(
            get("/1/receipts/R1.json?token=app-token")
                .willReturn(
                    okJson(
                        """
                            {
                                "status": 1,
                                "acknowledged": 1,
                                "acknowledged_at": %s,
                                "acknowledged_by": "user1",
                                "acknowledged_by_device": "device",
                                "last_delivered_at": %s,
                                "expired": 1,
                                "expires_at": %s,
                                "called_back": 0,
                                "called_back_at": 0,
                                "request": "D"
                            }
                            """.formatted(
                            deliveredAt.toInstant(ZoneOffset.UTC).getEpochSecond(),
                            deliveredAt.toInstant(ZoneOffset.UTC).getEpochSecond(),
                            expiresAt.toInstant(ZoneOffset.UTC).getEpochSecond()
                        )
                    )
                )
        );
    }

    private String partAsString(LoggedRequest request, String name) {
        final Optional<Request.Part> first = request.getParts().stream().filter(r -> r.getName().equals(name)).findFirst();
        return first.isPresent() ? first.get().getBody().asString() : "";
    }

    private boolean hasPart(LoggedRequest request, String name) {
        return request.getParts().stream().anyMatch(part -> part.getName().equals(name));
    }

    private void assertEncryptedValue(
        LoggedRequest request,
        String name,
        String expected,
        @SuppressWarnings("SameParameterValue") String encryptionKey
    )
        throws GeneralSecurityException, IOException {
        final String encryptedValue = partAsString(request, name);
        assertNotEquals(expected, encryptedValue);
        assertEquals(expected, decrypt(encryptedValue, encryptionKey));
    }

    private String decrypt(String encryptedValue, String encryptionKey) throws GeneralSecurityException, IOException {
        final byte[] key = hexDecode(encryptionKey);
        final byte[] payload = Base64.getDecoder().decode(encryptedValue);
        final byte[] iv = Arrays.copyOfRange(payload, 0, 16);
        final byte[] ciphertext = Arrays.copyOfRange(payload, 16, payload.length - 32);
        final byte[] hmac = Arrays.copyOfRange(payload, payload.length - 32, payload.length);

        final Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update(iv);
        mac.update(ciphertext);
        assertTrue(MessageDigest.isEqual(hmac, mac.doFinal()));

        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(cipher.doFinal(ciphertext)))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private byte[] hexDecode(String value) {
        final byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
