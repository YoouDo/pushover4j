package de.kleinkop.pushover4j.http;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import de.kleinkop.pushover4j.ReceiptResponse;
import de.kleinkop.pushover4j.Utils;

@JsonPropertyOrder(
    {"status","request","lastDeliveredAt","expiresAt","acknowledged","acknowledgedAt","acknowledgedBy","acknowledgedByDevice","expired","calledBack","calledBackAt"}
)
public record RawReceiptResponse(
    int status,
    String request,
    @JsonProperty(value = "last_delivered_at") Long lastDeliveredAt,
    @JsonProperty("expires_at") Long expiresAt,
    @JsonProperty int acknowledged,
    @JsonProperty("acknowledged_at") Long acknowledgedAt,
    @JsonProperty("acknowledged_by") String acknowledgedBy,
    @JsonProperty("acknowledged_by_device") String acknowledgedByDevice,
    @JsonProperty int expired,
    @JsonProperty("called_back") int calledBack,
    @JsonProperty("called_back_at") Long calledBackAt
) {
    public ReceiptResponse toDomain() {
        return new ReceiptResponse(
            status,
            request,
            Utils.toLocalDateTimeOrNull(lastDeliveredAt),
            Utils.toLocalDateTimeOrNull(expiresAt),
            acknowledged == 1,
            Utils.toLocalDateTimeOrNull(acknowledgedAt),
            Utils.nullable(acknowledgedBy),
            Utils.nullable(acknowledgedByDevice),
            expired == 1,
            calledBack == 1,
            Utils.toLocalDateTimeOrNull(calledBackAt)
        );
    }
}
