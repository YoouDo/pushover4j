package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.ApplicationUsage;
import de.kleinkop.pushover4j.PushoverResponse;
import de.kleinkop.pushover4j.Utils;

import java.net.http.HttpHeaders;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        List<String> validatedParams = Stream.of(
                "X-Limit-App-Limit",
                "X-Limit-App-Remaining",
                "X-Limit-App-Reset"
            )
            .map(header -> headers.firstValue(header).orElse(null))
            .filter(Objects::nonNull)
            .toList();

        if (validatedParams.size() == 3) {
            return new ApplicationUsage(
                Integer.valueOf(validatedParams.get(0)),
                Integer.valueOf(validatedParams.get(1)),
                Utils.toLocalDateTimeUTC(Long.valueOf(validatedParams.get(2)))
            );
        } else {
            return null;
        }
    }
}
