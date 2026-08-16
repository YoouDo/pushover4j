package de.kleinkop.pushover4j;

public interface PushoverClient {
    PushoverResponse sendMessage(Message msg);

    /**
     * Sends a message with its message, title, URL, and URL title fields encrypted using the supplied
     * 64-character hexadecimal Pushover encryption key.
     *
     * @param msg the message to send
     * @param encryptionKey the 256-bit Pushover encryption key as 64 hexadecimal characters
     * @return the Pushover response
     */
    PushoverResponse sendEncryptedMessage(Message msg, String encryptionKey);

    SoundResponse getSounds();

    ReceiptResponse getEmergencyState(String receiptId);

    PushoverResponse cancelEmergencyMessage(String receiptId);

    PushoverResponse cancelEmergencyMessageByTag(String tag);
}
