package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.ApplicationUsage;
import de.kleinkop.pushover4j.PushoverResponse;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record RawPushoverResponse(
    int status,
    String request,
    String user,
    List<String> errors,
    String receipt,
    Integer canceled
) {
    public PushoverResponse toDomain(HttpHeaders headers) {
        return new PushoverResponse(
            status,
            request,
            user,
            new ArrayList<>(errors != null ? errors : new ArrayList<>()),
            receipt,
            canceled,
            extractApplicationUsage(headers)
        );
    }

    private ApplicationUsage extractApplicationUsage(HttpHeaders headers) {
        final Map<String, Optional<String>> params = Stream.of(
            ApplicationUsage.KEY_LIMIT,
                ApplicationUsage.KEY_REMAINING,
                ApplicationUsage.KEY_RESET
            )
            .collect(Collectors.toMap(Function.identity(), headers::firstValue));

        return ApplicationUsage.from(params);
    }
}
