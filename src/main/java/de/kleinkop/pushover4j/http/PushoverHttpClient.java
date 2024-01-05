package de.kleinkop.pushover4j.http;

import de.kleinkop.pushover4j.Message;
import de.kleinkop.pushover4j.PushoverClient;
import de.kleinkop.pushover4j.PushoverResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.UUID;

public class PushoverHttpClient implements PushoverClient {
    private static final Logger logger = LoggerFactory.getLogger(PushoverHttpClient.class);

    private static Long HTTP_TIMEOUT_IN_SECONDS = 30L;

    private String appToken;
    private String userToken;
    private String apiHost = "https://api.pushover.net";
    private Long httpTimeout = HTTP_TIMEOUT_IN_SECONDS;

    final private String url;

    final private HttpClient httpClient;

    public PushoverHttpClient(String appToken, String userToken, String apiHost, Long httpTimeout) {
        this.appToken = appToken;
        this.userToken = userToken;
        if (apiHost != null && !apiHost.isBlank()) {
            this.apiHost = apiHost;
        }
        if (httpTimeout != null) {
            this.httpTimeout = httpTimeout;
        }

        url = this.apiHost + "/1/messages.json";

        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(this.httpTimeout))
            .build();
    }

    private HttpRequest.Builder defaultRequest(String url) throws URISyntaxException {
        return HttpRequest.newBuilder()
            .uri(new URI(url))
            .timeout(Duration.ofSeconds(15L))
            .version(HttpClient.Version.HTTP_1_1);
    }
    @Override
    public PushoverResponse sendMessage(Message msg) {
        try {
            final MultipartFormData bodyData = new HttpClientMultipartFormBody()
                .plus("token", this.appToken)
                .plus("user", this.userToken)
                .plus("message", msg.getMessage())
                .plus("priority", "" + msg.getPriority().getValue())
                .plusIfSet("title", msg.getTitle())
                .plusIfSet("url", msg.getUrl())
                .plusIfSet("url_title", msg.getUrlTitle())
                .plusIfSet("html", msg.isHtml(), "1")
                .plusIfSet("monospace", msg.isMonospace(), "1")
                .plusIfSet("sound", msg.getSound())
                .plusIfSet(
                    "timestamp",
                    msg.getTimestamp() != null,
                    "" + msg.getTimestamp().toEpochSecond(ZoneOffset.UTC)
                )
                .plusIfSet("device", msg.getDevices())
                .plusIfSet("retry", "" + msg.getRetry())
                .plusIfSet("expire", "" + msg.getExpire())
                .plusIfSet("tags", msg.getTags())
                .plusImageIfSet("attachment", msg.getImage())
                .build(UUID.randomUUID().toString());

            final HttpRequest request = defaultRequest(url)
                .header("Content-Type", bodyData.getBoundary())
                .POST(
                    HttpRequest.BodyPublishers.ofByteArrays(bodyData.getByteArrays())
                )
                .build();

            final HttpResponse<String> response = httpRequest(request);
            logger.info("Response: " + response.body());
            return new PushoverResponse(response.body());
        } catch (Exception e) {
            logger.warn("Error while sending request", e);
            throw new RuntimeException("Sending message to Pushover failed", e);
        }
    }

    private HttpResponse<String> httpRequest(HttpRequest request) throws IOException, InterruptedException {
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 500) {
            throw new RuntimeException("Call to Pushover API failed with status code " + response.statusCode());
        }
        return response;
    }
}
