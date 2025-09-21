package de.kleinkop.pushover4j.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLException;

public class HttpRetry {
    private static final Logger logger = LoggerFactory.getLogger(HttpRetry.class);

    final private HttpClient client;
    final private int maxRetries;
    private final long retryInterval;

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
            logger.info("Retry attempt {}/{} in {} ms", count, maxRetries, retryInterval);
            return CompletableFuture
                .runAsync(() -> {}, CompletableFuture.delayedExecutor(retryInterval, TimeUnit.MILLISECONDS))
                .thenCompose(v -> client.sendAsync(request, handler)
                    .handle((r, x) -> tryResend(client, request, handler, count + 1, r, x))
                    .thenCompose(Function.identity())
                );
        } else if (t != null) {
            logger.debug("Returning error (no more retries)", t);
            return CompletableFuture.failedFuture(
                new RetryException(-1, null, t)
            );
        } else if (isFailure(resp)) {
            final int status = resp != null ? resp.statusCode() : -1;
            final String body = resp != null ? resp.body() : null;
            logger.warn("HTTP call failed after {} attempt(s): status={}, body={}", count, status, body);
            return CompletableFuture.failedFuture(
                new RetryException(status, body)
            );
        } else {
            logger.debug("Successful retry: {}", resp.body());
            return CompletableFuture.completedFuture(resp);
        }
    }

    public boolean shouldRetry(HttpResponse<?> r, Throwable t, int count) {
        if (count > maxRetries) {
            return false;
        }
        if (t != null) {
            return isTransientException(t);
        }
        return isTransientFailure(r);
    }

    private boolean isTransientFailure(HttpResponse<?> r) {
        return r == null || TransientHttpErrors.isTransient(r.statusCode());
    }

    private boolean isFailure(HttpResponse<?> r) {
        return r == null || r.statusCode() / 100 != 2;
    }

    private boolean isTransientException(Throwable t) {
        // Retry only for commonly transient network conditions
        return t instanceof ConnectException
            || t instanceof HttpTimeoutException
            || t instanceof SSLException
            || t instanceof InterruptedIOException;
    }
}
