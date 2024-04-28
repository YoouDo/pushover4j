package de.kleinkop.pushover4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapper {
    private static final ObjectMapper mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
            return mapper.readValue(json, clazz);
    }

    public static String toJsonOrNull(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "";
        }
    }
}
