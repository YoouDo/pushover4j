package de.kleinkop.pushover4j;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import de.kleinkop.pushover4j.http.PushoverHttpClient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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
        pushoverClient = new PushoverHttpClient(
            "app-token",
            "user-token",
            5,
            10L
        ).withApiHost("http://localhost:" + runtimeInfo.getHttpPort())
            .withHttpTimeout(10L);

        invalidClient = new PushoverHttpClient(
            "invalid-token",
            null
        ).withApiHost("http://localhost:" + runtimeInfo.getHttpPort());
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
        assertEquals("application token is invalid", sounds.errors().get(0));
        assertEquals("error-request", sounds.request());
    }

    @Test
    void testInvalidMessages() {
        // emergency msg without retry and expiration
        assertThrows(
            IllegalArgumentException.class,
            () -> Message.of("msg")
                .withPriority(Priority.EMERGENCY)
                .build()
        );

        // emergency msg without expiration
        assertThrows(
            IllegalArgumentException.class,
            () -> Message.of("msg")
                .withPriority(Priority.EMERGENCY)
                .withRetry(30)
                .build()
        );

        // emergency msg without retry
        assertThrows(
            IllegalArgumentException.class,
            () -> Message.of("msg")
                .withPriority(Priority.EMERGENCY)
                .withExpiration(60)
                .build()
        );

        // Message with options html and monospace
        assertThrows(
            IllegalArgumentException.class,
            () -> Message.of("msg")
                .withHtml(true)
                .withMonospace(true)
                .build()
        );
    }

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
        assertEquals(Utils.toLocalDateTimeUTC(1393653600L), response.applicationUsage().reset());

        final List<LoggedRequest> loggedRequests = findAll(postRequestedFor(urlPathEqualTo("/1/messages.json")))
            .stream()
            .filter(LoggedRequest::isMultipart)
            .toList();

        assertEquals(1, loggedRequests.size());
        assertEquals("app-token", partAsString(loggedRequests.get(0), "token"));
        assertEquals("user-token", partAsString(loggedRequests.get(0), "user"));
        assertEquals("Testing", partAsString(loggedRequests.get(0), "message"));
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

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> pushoverClient.sendMessage(Message.of("Testing").build()));
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


}
