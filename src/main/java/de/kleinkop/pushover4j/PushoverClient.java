package de.kleinkop.pushover4j;

public interface PushoverClient {
    PushoverResponse sendMessage(Message msg);
}
