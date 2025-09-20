package de.kleinkop.pushover4j;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public record ApplicationUsage(
    Integer limit,
    Integer remaining,
    OffsetDateTime reset
) {
    public static String KEY_LIMIT = "X-Limit-App-Limit";
    public static String KEY_REMAINING = "X-Limit-App-Remaining";
    public static String KEY_RESET = "X-Limit-App-Reset";

    public static ApplicationUsage from(Map<String, Optional<String>> keyValues) {
        return new ApplicationUsage(
            keyValues.get(KEY_LIMIT).map(Integer::parseInt).orElse(null),
            keyValues.get(KEY_REMAINING).map(Integer::parseInt).orElse(null),
            keyValues.get(KEY_RESET).map(Long::parseLong).map(Utils::toOffsetDateTimeUTC).orElse(null)
        );
    }
}
