package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.JsonMapper;
import de.kleinkop.pushover4j.TestUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SerDeTest {
    @Test
    void testRawSoundResponse() throws Exception {
        String json = TestUtils.readResourceAsString("/testdata/sounds.json");
        final RawSoundResponse rawSoundResponse = JsonMapper.fromJson(json, RawSoundResponse.class);

        assertNotNull(rawSoundResponse);
        assertEquals(1, rawSoundResponse.status());
        assertEquals("A", rawSoundResponse.request());
        assertEquals("description", rawSoundResponse.sounds().get("soundname"));
        assertNull(rawSoundResponse.errors());
        assertNull(rawSoundResponse.token());
    }
}
