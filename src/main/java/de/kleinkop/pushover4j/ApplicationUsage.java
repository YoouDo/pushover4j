package de.kleinkop.pushover4j;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public record ApplicationUsage(
    Integer limit,
    Integer remaining,
    OffsetDateTime reset
) {
    public static final String KEY_LIMIT = "X-Limit-App-Limit";
    public static final String KEY_REMAINING = "X-Limit-App-Remaining";
    public static final String KEY_RESET = "X-Limit-App-Reset";

    public static ApplicationUsage from(Map<String, Optional<String>> keyValues) {
        return new ApplicationUsage(
            keyValues.get(KEY_LIMIT)
                .flatMap(ApplicationUsage::parseInt)
                .filter(v -> v >= 0)
                .orElse(null),
            keyValues.get(KEY_REMAINING)
                .flatMap(ApplicationUsage::parseInt)
                .filter(v -> v >= 0)
                .orElse(null),
            keyValues.get(KEY_RESET)
                .flatMap(ApplicationUsage::parseLong)
                .filter(v -> v >= 0L)
                .map(Utils::toOffsetDateTimeUTC)
                .orElse(null)
        );
    }

    private static Optional<Integer> parseInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
