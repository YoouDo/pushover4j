package de.kleinkop.pushover4j.http;

public class RetryException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public RetryException(int statusCode, String responseBody) {
        super("Call failed with status=%d body=%s".formatted(statusCode, responseBody));
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public RetryException(int statusCode, String responseBody, Throwable cause) {
        super("Call failed with status=%d body=%s".formatted(statusCode, responseBody), cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
