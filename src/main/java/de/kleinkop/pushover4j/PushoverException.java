package de.kleinkop.pushover4j;

public class PushoverException extends RuntimeException {
    public PushoverException(String message) {
        super(message);
    }

    public PushoverException(String message, Throwable cause) {
        super(message, cause);
    }
}
