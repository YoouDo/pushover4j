package de.kleinkop.pushover4j;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class Utils {

    public static String nullable(String text) {
        if ("".equals(text)) {
            return null;
        }
        return text;
    }

    public static LocalDateTime toLocalDateTimeUTC(Long epochSecond) {
        return LocalDateTime.ofEpochSecond(epochSecond, 0, ZoneOffset.UTC);
    }

    public static LocalDateTime toLocalDateTimeOrNull(Long epochSecond) {
        if (epochSecond == null || epochSecond == 0L) {
            return null;
        }
        return toLocalDateTimeUTC(epochSecond);
    }

    public static LocalDateTime toLocalDateTimeUTC(OffsetDateTime dateTime) {
        return dateTime
            .atZoneSameInstant(ZoneId.of("UTC"))
            .toLocalDateTime();
    }
}
