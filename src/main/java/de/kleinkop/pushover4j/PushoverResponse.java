package de.kleinkop.pushover4j;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PushoverResponse {


    public static final Pattern STATUS_PATTERN = Pattern.compile("\"status\":(?<status>\\d+)");
    private Integer status;
    private String request;
    private String user;
    private List<String> errors;
    private String receipt;
    private Integer canceled;
    private ApplicationUsage applicationUsage;

    public PushoverResponse(String body) {
        initFromBody(body);
    }

    public Integer getStatus() {
        return status;
    }

    public String getRequest() {
        return request;
    }

    public String getUser() {
        return user;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getReceipt() {
        return receipt;
    }

    public Integer getCanceled() {
        return canceled;
    }

    public ApplicationUsage getApplicationUsage() {
        return applicationUsage;
    }

    private void initFromBody(String body) {
        findInteger(STATUS_PATTERN, "status", body).ifPresent(responseStatus -> this.status = responseStatus);
    }

    private Optional<Integer> findInteger(Pattern pattern, String group, String body) {
        final Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            final String found = matcher.group(group);
            return Optional.ofNullable(found)
                .map(Integer::valueOf);
        }
        return Optional.empty();
    }
}
