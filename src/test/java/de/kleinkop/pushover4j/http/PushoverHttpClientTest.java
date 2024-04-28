package de.kleinkop.pushover4j.http;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.kleinkop.pushover4j.JsonMapper;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PushoverHttpClientTest {
    @Test
    void serdeTest() throws JsonProcessingException {
        final RawPushoverResponse rawPushoverResponse = new RawPushoverResponse(
            1,
            "abc",
            "me",
            List.of("error"),
            "receipt",
            0
        );

        final String expected = """
            {
                "status": %s,
                "request": "%s",
                "user": "%s",
                "errors": ["%s"],
                "receipt": "%s",
                "canceled": %s
            }
            """.formatted(
            rawPushoverResponse.status(),
            rawPushoverResponse.request(),
            rawPushoverResponse.user(),
            rawPushoverResponse.errors().get(0),
            rawPushoverResponse.receipt(),
            rawPushoverResponse.canceled()
        )
            .replaceAll("[\\n ]", "");

        assertEquals(expected, JsonMapper.toJsonOrNull(rawPushoverResponse));

        assertEquals(rawPushoverResponse, JsonMapper.fromJson(expected, RawPushoverResponse.class));
    }

    @Test
    void serdeSoundTest() throws JsonProcessingException {
        final RawSoundResponse rawSoundResponse = new RawSoundResponse(
            1,
            "request",
            Map.of("a", "b"),
            List.of("error"),
            "token"
        );

        String expected = """
            {
                "status" : %s,
                "request": "%s",
                "sounds": {"a":"b"},
                "errors": ["%s"],
                "token": "%s"
            }
            """.formatted(
            rawSoundResponse.status(),
            rawSoundResponse.request(),
            rawSoundResponse.errors().get(0),
            rawSoundResponse.token()
        ).replaceAll("[\\n ]", "");

        assertEquals(expected, JsonMapper.toJsonOrNull(rawSoundResponse));
        assertEquals(rawSoundResponse, JsonMapper.fromJson(expected, RawSoundResponse.class));
    }

    @Test
    void serdeReceiptMessage() throws JsonProcessingException {
        final RawReceiptResponse rawReceiptMessage = new RawReceiptResponse(
            1,
            "abc",
            1234567890L,
            2234567890L,
            1,
            1234567890L,
            "me",
            "mobile",
            1,
            1,
            1234567890L
        );

        String expected = """
            {
               "status": 1,
                "request": "%s",
                "last_delivered_at": %s,
                "expires_at": %s,
                "acknowledged": %s,
                "acknowledged_at": %s,
                "acknowledged_by": "%s",
                "acknowledged_by_device": "%s",
                "expired": %s,
                "called_back": %s,
                "called_back_at": %s
            }
            """.formatted(
            rawReceiptMessage.request(),
            rawReceiptMessage.lastDeliveredAt(),
            rawReceiptMessage.expiresAt(),
            rawReceiptMessage.acknowledged(),
            rawReceiptMessage.acknowledgedAt(),
            rawReceiptMessage.acknowledgedBy(),
            rawReceiptMessage.acknowledgedByDevice(),
            rawReceiptMessage.expired(),
            rawReceiptMessage.calledBack(),
            rawReceiptMessage.calledBackAt()
        ).replaceAll("[\\n ]", "");

        assertEquals(expected, JsonMapper.toJsonOrNull(rawReceiptMessage));
        assertEquals(rawReceiptMessage, JsonMapper.fromJson(expected, RawReceiptResponse.class));
    }
}
