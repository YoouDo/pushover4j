package de.kleinkop.pushover4j.http;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.kleinkop.pushover4j.SoundResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize
public record RawSoundResponse(
    int status,
    String request,
    Map<String,String> sounds,
    List<String> errors,
    String token
) {
    public SoundResponse toDomain() {
        return new SoundResponse(
            status,
            request,
            sounds == null ? null : new HashMap<>(sounds),
            errors == null ? null : new ArrayList<>(errors),
            token
        );
    }
}
