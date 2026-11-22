package de.kleinkop.pushover4j.http;

public record PushoverApiPaths(
    String apiHost,
    String message,
    String sounds,
    String receipt,
    String cancel,
    String cancelByTag
){

    public static final String DEFAULT_API_HOST = "https://api.pushover.net";
    public static final String DEFAULT_MSG_PATH = "/1/messages.json";
    public static final String DEFAULT_SOUNDS_PATH = "/1/sounds.json";
    public static final String DEFAULT_RECEIPT_PATH = "/1/receipts/";
    public static final String DEFAULT_CANCEL_PATH = "/1/receipts/{RECEIPT_ID}/cancel.json";
    public static final String DEFAULT_CANCEL_BY_TAG_PATH = "/1/receipts/cancel_by_tag/";

    public static PushoverApiPaths defaults() {
        return defaultsWithApiHost(DEFAULT_API_HOST);
    }

    public static PushoverApiPaths defaultsWithApiHost(String apiHost) {
        return new PushoverApiPaths(
            apiHost,
            DEFAULT_MSG_PATH,
            DEFAULT_SOUNDS_PATH,
            DEFAULT_RECEIPT_PATH,
            DEFAULT_CANCEL_PATH,
            DEFAULT_CANCEL_BY_TAG_PATH
        );
    }

    /**
     * @return Full message path
     */
    public String messagePath() {
        return apiHost + message;
    }

    public String soundsPath() {
        return apiHost + sounds;
    }

    public String receiptPath() {
        return apiHost + receipt;
    }

    public String cancelPath() {
        return apiHost + cancel;
    }

    public String cancelByTagPath() {
        return apiHost + cancelByTag;
    }
}
