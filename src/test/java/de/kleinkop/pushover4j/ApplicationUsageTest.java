package de.kleinkop.pushover4j;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ApplicationUsageTest {
    @Test
    void testCreateFromMap() {
        var dateTime = OffsetDateTime.of(
            LocalDate.of(2026, 11, 1),
            LocalTime.MIN,
            ZoneOffset.UTC
        );
        Map<String, Optional<String>> params = Map.of(
            ApplicationUsage.KEY_LIMIT, Optional.of("10"),
            ApplicationUsage.KEY_REMAINING, Optional.of("5"),
            ApplicationUsage.KEY_RESET, Optional.of("" + dateTime.toEpochSecond())
        );
        ApplicationUsage usage = ApplicationUsage.from(params);
        assertEquals(10, usage.limit());
        assertEquals(5, usage.remaining());
        assertEquals(dateTime, usage.reset());
    }

    @Test
    void testInvalidValues() {
        Map<String, Optional<String>> params = Map.of(
            ApplicationUsage.KEY_LIMIT, Optional.of("10a"),
            ApplicationUsage.KEY_REMAINING, Optional.of("5b"),
            ApplicationUsage.KEY_RESET, Optional.of("1c")
        );
        ApplicationUsage usage = ApplicationUsage.from(params);
        assertNull(usage.limit());
        assertNull(usage.remaining());
        assertNull(usage.reset());
    }
}
