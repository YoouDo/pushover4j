package de.kleinkop.pushover4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PushoverResponseTest {
    @Test
    void testResponseParsing() {
        PushoverResponse r = new PushoverResponse("{\"status\":1,\"request\":\"2d7dc106-6c74-4e29-af48-be9201e12782\"}");
        assertEquals(1, r.getStatus());
    }
}
