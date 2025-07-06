package de.kleinkop.pushover4j.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public class HttpClientMultipartFormBody {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientMultipartFormBody.class);

    private final Map<String, Object> content = new LinkedHashMap<>();

    public HttpClientMultipartFormBody plus(String name, String value) {
        content.put(name, value);
        return this;
    }

    public HttpClientMultipartFormBody plusIfSet(String name, Object value) {
        if (value != null) {
            return plus(name, String.valueOf(value));
        }
        return this;
    }

    public HttpClientMultipartFormBody plusIfSet(String name, boolean isSet, String value) {
        if (isSet) {
            return plus(name, value);
        }
        return this;
    }

    public HttpClientMultipartFormBody plusIfSet(String name, boolean isSet, Supplier<String> valueProvider) {
        if (isSet) {
            return plus(name, valueProvider.get());
        }
        return this;
    }

    public HttpClientMultipartFormBody plusIfSet(String name, String value) {
        if (value != null && !value.isBlank()) {
            return plus(name, value);
        }
        return this;
    }

    public HttpClientMultipartFormBody plusIfSet(String name, Collection<String> values) {
        if (values != null && !values.isEmpty()) {
            return plus(name, String.join(",", values));
        }
        return this;
    }

    public HttpClientMultipartFormBody plusImageIfSet(String name, File file) {
        if (file != null) {
            content.put(name, file);
        }
        return this;
    }

    private byte[] fileEntry(String key, String fileName, String mimeType) {
        return ("\"" +
            key +
            "\"; filename=\"" +
            fileName +
            "\"\r\nContent-Type: " +
            mimeType +
            "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] textEntry(String key, String value) {
        final byte[] result = ("\"" +
            key +
            "\"\r\n\r\n" +
            value +
            "\r\n").getBytes(StandardCharsets.UTF_8);

        if ("token".equals(key) && logger.isDebugEnabled()) {
            logger.debug("token={}", value);
            logger.debug(Arrays.toString(result));
        }
        return result;
    }

    public MultipartFormData build(final String boundary) {
        final List<byte[]> byteArrays = new ArrayList<>();
        final byte[] separator = ("--" + boundary + "\r\n" +
            "Content-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);


        for (Map.Entry<String, Object> entry : content.entrySet()) {
            byteArrays.add(separator);
            if (entry.getValue() instanceof File file) {
                try {
                    final Path path = Path.of(file.toURI());
                    final String mimeType = Files.probeContentType(path);
                    byteArrays.add(
                        fileEntry(
                            entry.getKey(),
                            path.getFileName().toString(),
                            mimeType
                        )
                    );
                    byteArrays.add(Files.readAllBytes(path));
                    byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
                } catch (IOException ioException) {
                    // ignore
                }
            } else {
                byteArrays.add(
                    textEntry(entry.getKey(), entry.getValue().toString())
                );
            }
        }
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return new MultipartFormData(
            "multipart/form-data;boundary=" + boundary,
            byteArrays
        );
    }
}
