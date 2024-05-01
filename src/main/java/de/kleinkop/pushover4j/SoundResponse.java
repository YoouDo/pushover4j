package de.kleinkop.pushover4j;

import java.util.List;
import java.util.Map;

public record SoundResponse(
    int status,
    String request,
    Map<String,String> sounds,
    List<String> errors,
    String token
) {}
