package de.kleinkop.pushover4j.http;

import java.util.List;

public class MultipartFormData {
    private String boundary;
    private List<byte[]> byteArrays;

    public MultipartFormData(String boundary, List<byte[]> byteArrays) {
        this.boundary = boundary;
        this.byteArrays = byteArrays;
    }

    public String getBoundary() {
        return boundary;
    }

    public List<byte[]> getByteArrays() {
        return byteArrays;
    }
}
