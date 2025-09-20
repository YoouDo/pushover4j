package de.kleinkop.pushover4j;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void nullable_convertsEmptyStringToNull_andKeepsOthers() {
        assertNull(Utils.nullable(""));
        assertEquals("a", Utils.nullable("a"));
        assertNull(Utils.nullable(null));
    }

    @Test
    void toLocalDateTimeUTC_fromEpochSecond() {
        long epoch = 1_700_000_000L; // fixed point in time
        LocalDateTime ldt = Utils.toLocalDateTimeUTC(epoch);
        assertEquals(LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC), ldt);
    }

    @Test
    void toOffsetDateTimeUTC_fromEpochSecond() {
        long epoch = 1_700_000_000L;
        OffsetDateTime odt = Utils.toOffsetDateTimeUTC(epoch);
        assertEquals(OffsetDateTime.of(LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC), ZoneOffset.UTC), odt);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
    }

    @SuppressWarnings("ConstantValue")
    @Test
    void toLocalDateTimeOrNull_handlesNullAndZero() {
        assertNull(Utils.toLocalDateTimeOrNull(null));
        assertNull(Utils.toLocalDateTimeOrNull(0L));

        long epoch = 1_650_000_000L;
        LocalDateTime expected = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC);
        assertEquals(expected, Utils.toLocalDateTimeOrNull(epoch));
    }

    @Test
    void toLocalDateTimeUTC_fromOffsetDateTime_normalizesToUTC() {
        // 2023-01-01T12:00:00+02:00 should become 2023-01-01T10:00:00 UTC
        OffsetDateTime source = OffsetDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneOffset.ofHours(2));
        LocalDateTime utc = Utils.toLocalDateTimeUTC(source);

        LocalDateTime expected = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        assertEquals(expected, utc);
    }
}
