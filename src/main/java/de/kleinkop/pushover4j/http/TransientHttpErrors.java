package de.kleinkop.pushover4j.http;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TransientHttpErrors {
    // List of HTTP response codes commonly considered transient
    public static final List<Integer> TRANSIENT_STATUS_CODES = List.of(
        408, // Request Timeout
        425, // Too Early
        429, // Too Many Requests
        500, // Internal Server Error
        502, // Bad Gateway
        503, // Service Unavailable
        504  // Gateway Timeout
    );

    // If you also want fast membership checks for isTransient(code)
    public static final Set<Integer> TRANSIENT_STATUS_SET =
        TRANSIENT_STATUS_CODES.stream().collect(Collectors.toUnmodifiableSet());

    public static boolean isTransient(int statusCode) {
        return TRANSIENT_STATUS_SET.contains(statusCode);
    }
}
