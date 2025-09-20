package de.kleinkop.pushover4j;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonMapper {
    private static final Logger logger = LoggerFactory.getLogger(JsonMapper.class);

    private static final ObjectMapper mapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T fromJson(String json, Class<T> clazz) throws JsonProcessingException {
            return mapper.readValue(json, clazz);
    }

    public static String toJsonOrNull(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to serialize object to JSON", e);
            return null;
        }
    }
}
