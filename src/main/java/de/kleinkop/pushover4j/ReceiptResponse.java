package de.kleinkop.pushover4j;

import java.time.LocalDateTime;

public record ReceiptResponse(
    int status,
    String request,
    LocalDateTime lastDeliveredAt,
    LocalDateTime expirersAt,
    boolean acknowledged,
    LocalDateTime acknowledgedAt,
    String acknowledgedBy,
    String acknowledgedByDevice,
    boolean expired,
    boolean calledBack,
    LocalDateTime calledBackAt
) {
}
