package de.kleinkop.pushover4j.http;

import java.util.List;

public record MultipartFormData(String boundary, List<byte[]> byteArrays) {}
