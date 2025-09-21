package de.kleinkop.pushover4j.http;

public class RetryException extends Exception {
    private final Integer code;
    private final String serverMessage;

    public RetryException(Integer code, String cause) {
        super();
        this.serverMessage = cause;
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public String getServerMessage() {
        return serverMessage;
    }

    @Override
    public String toString() {
        return "Call to Pushover API failed: Code %d, Error: %s"
            .formatted(code, serverMessage);
    }
}
