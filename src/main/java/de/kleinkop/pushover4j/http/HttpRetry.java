package de.kleinkop.pushover4j.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class HttpRetry {
    private static final Logger logger = LoggerFactory.getLogger(HttpRetry.class);

    private HttpClient client;
    private int maxRetries;
    private long retryInterval;

    public HttpRetry(HttpClient client, int maxRetries, long retryInterval) {
        this.client = client;
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    public HttpResponse<String> send(HttpRequest request, HttpResponse.BodyHandler<String> responseBodyHandler) {
        return client.sendAsync(request, responseBodyHandler)
            .handle((r, t) -> tryResend(client, request, responseBodyHandler, 1, r, t))
            .thenCompose(Function.identity())
            .join();
    }

    private CompletableFuture<HttpResponse<String>> tryResend(
        HttpClient client,
        HttpRequest request,
        HttpResponse.BodyHandler<String> handler,
        int count,
        HttpResponse<String> resp,
        Throwable t
    ) {
        if (shouldRetry(resp, t, count)) {
            logger.debug("About to call retry for {} times", count);
            return client.sendAsync(request, handler)
                .handleAsync((r, x) -> tryResend(client, request, handler, count + 1, r, x))
                .thenCompose(Function.identity());
        } else if (t != null) {
            logger.debug("Returning error", t);
            return CompletableFuture.failedFuture(t);
        } else if (resp.statusCode() / 100 != 2) {
            return CompletableFuture.failedFuture(new RuntimeException("Call to Pushover API failed: " + resp.statusCode()));
        } else {
            logger.debug("Successful retry: {}", resp.body());
            return CompletableFuture.completedFuture(resp);
        }
    }

    public boolean shouldRetry(HttpResponse<?> r, Throwable t, int count) {
        return (r == null || r.statusCode() != 200) && count < maxRetries;
    }
}
