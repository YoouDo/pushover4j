package de.kleinkop.pushover4j.http;

public record BodyToken(
    String token
) {
    @Override
    public String toString() {
        return "token=" + token;
    }
}
