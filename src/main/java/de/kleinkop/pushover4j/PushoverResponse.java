package de.kleinkop.pushover4j;

import java.util.List;

public record PushoverResponse(
    int status,
    String request,
    String user,
    List<String> errors,
    String receipt,
    Integer canceled,
    ApplicationUsage applicationUsage
) {}
