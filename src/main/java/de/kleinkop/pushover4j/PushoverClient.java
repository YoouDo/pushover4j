package de.kleinkop.pushover4j;

public interface PushoverClient {
    PushoverResponse sendMessage(Message msg);

    SoundResponse getSounds();

    ReceiptResponse getEmergencyState(String receiptId);

    PushoverResponse cancelEmergencyMessage(String receiptId);

    PushoverResponse cancelEmergencyMessageByTag(String tag);
}
