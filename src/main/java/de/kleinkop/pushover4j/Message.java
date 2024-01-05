package de.kleinkop.pushover4j;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Message {
    private String message;
    private String title;
    private Priority priority = Priority.NORMAL;
    private String url;
    private String urlTitle;
    private List<String> devices = new ArrayList<>();
    private LocalDateTime timestamp;
    private boolean html = true;
    private String sound;
    private File image;
    private boolean monospace = false;
    private Integer retry;
    private Integer expire;
    private List<String> tags = new ArrayList<>();

    private Message(String msg) {
        this.message = msg;
    }

    public Message(String message, String title, Priority priority, String url, String urlTitle, List<String> devices, LocalDateTime timestamp, boolean html, String sound, File image, boolean monospace, Integer retry, Integer expire) {
        this.message = message;
        this.title = title;
        this.priority = priority;
        this.url = url;
        this.urlTitle = urlTitle;
        this.devices = devices;
        this.timestamp = timestamp;
        this.html = html;
        this.sound = sound;
        this.image = image;
        this.monospace = monospace;
        this.retry = retry;
        this.expire = expire;
        if (priority == Priority.EMERGENCY) {
            Objects.requireNonNull(retry, "Retry value required for emergency messages");
            Objects.requireNonNull(expire, "Expiration value required for emergency messages");
        }

        if (html && monospace) {
            throw new IllegalArgumentException("Don't use options html and monospace together");
        }
    }

    public String getMessage() {
        return message;
    }

    public String getTitle() {
        return title;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getUrl() {
        return url;
    }

    public String getUrlTitle() {
        return urlTitle;
    }

    public List<String> getDevices() {
        return devices;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isHtml() {
        return html;
    }

    public String getSound() {
        return sound;
    }

    public File getImage() {
        return image;
    }

    public boolean isMonospace() {
        return monospace;
    }

    public Integer getRetry() {
        return retry;
    }

    public Integer getExpire() {
        return expire;
    }

    public List<String> getTags() {
        return tags;
    }

    public static Builder of(String msg) {
        return new Builder(msg);
    }

    public static class Builder {
        final private Message template;

        private Builder(String msg) {
            this.template = new Message(msg);
        }

        public Builder withTitle(String title) {
            this.template.title = title;
            return this;
        }

        public Builder withPriority(Priority priority) {
            this.template.priority = priority;
            return this;
        }

        public Builder withUrl(String url) {
            this.template.url = url;
            return this;
        }

        public Builder withUrlTitle(String urlTitle) {
            this.template.urlTitle = urlTitle;
            return this;
        }

        public Builder withDevice(String device) {
            this.template.devices.add(device);
            return this;
        }

        public Builder withTag(String tag) {
            this.template.tags.add(tag);
            return this;
        }

        public Builder withTimestamp(OffsetDateTime dateTime) {
            this.template.timestamp = Utils.toLocalDateTimeUTC(dateTime);
            return this;
        }

        public Builder withHtml(boolean html) {
            this.template.html = html;
            return this;
        }

        public Builder withSound(String sound) {
            this.template.sound = sound;
            return this;
        }

        public Builder withImage(File file) {
            this.template.image = file;
            return this;
        }

        public Builder withMonospace(boolean monospace) {
            this.template.monospace = monospace;
            return this;
        }

        public Builder withRetry(int retry) {
            this.template.retry = retry;
            return this;
        }

        public Builder withExpiration(int expiration) {
            this.template.expire = expiration;
            return this;
        }

        public Message build() {
            return template;
        }
    }
}
