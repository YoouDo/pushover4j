package de.kleinkop.pushover4j;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Message {
    @SuppressWarnings("java:S1700")
    private final String message;
    private String title;
    private Priority priority = Priority.NORMAL;
    private String url;
    private String urlTitle;
    private final Set<String> devices = new HashSet<>();
    private OffsetDateTime timestamp;
    private boolean html = true;
    private String sound;
    private File image;
    private boolean monospace = false;
    private Integer retry;
    private Integer expire;
    private final Set<String> tags = new HashSet<>();
    private Integer ttl;

    private Message(String msg) {
        this.message = msg;
    }

    private void validate() {
        if (priority == Priority.EMERGENCY) {
            if (retry == null || retry < 0) {
                throw new IllegalArgumentException("Retry value required for emergency messages");
            }
            if (expire == null || expire < 0) {
                throw new IllegalArgumentException("Expiration value required for emergency messages");
            }
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

    public Set<String> getDevices() {
        return devices;
    }

    public OffsetDateTime getTimestamp() {
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

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public Integer getTtl() {
        return ttl;
    }

    public static Builder of(String msg) {
        return new Builder(msg);
    }

    public static class Builder {
        final private Message template;

        private Builder(String msg) {
            this.template = new Message(msg);
        }

        /**
         * Sets the title for the message being built.
         *
         * @param title the title to be set for the message
         * @return the {@code Builder} instance to allow method chaining
         */
        public Builder withTitle(String title) {
            this.template.title = title;
            return this;
        }

        /**
         * Sets the priority for the message being built.
         *
         * @param priority the priority level to be assigned to the message
         * @return the {@code Builder} instance to allow method chaining
         */
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
            this.template.timestamp = dateTime;
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

        /**
         * Sets the retry interval for the emergency message being built.
         * This value specifies the time in seconds between retry attempts.
         *
         * @param retry the retry interval in seconds
         * @return the {@code Builder} instance to allow method chaining
         */
        public Builder withRetry(int retry) {
            this.template.retry = retry;
            return this;
        }

        /**
         * Sets the expiration time for the emergency message being built.
         * This value indicates the duration, in seconds, after which the message becomes invalid.
         *
         * @param expiration the expiration duration in seconds
         * @return the {@code Builder} instance to enable method chaining
         */
        public Builder withExpiration(int expiration) {
            this.template.expire = expiration;
            return this;
        }

        /**
         * Sets the time-to-live (TTL) value for the message being built. This value determines
         * how long the message will remain active before it expires.
         *
         * @param ttl the time-to-live value in seconds to be set for the message
         * @return the {@code Builder} instance to allow method chaining
         */
        public Builder withTtl(int ttl) {
            this.template.ttl = ttl;
            return this;
        }

        public Message build()
        {
            template.validate();
            return template;
        }
    }
}
