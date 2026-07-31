package ru.sultanyarov.configurator.domain.model;

import java.util.Arrays;

public record ComponentImageUpload(byte[] content, String contentType) {
    public ComponentImageUpload {
        content = content == null ? null : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return content == null ? null : Arrays.copyOf(content, content.length);
    }

    public int size() {
        return content == null ? 0 : content.length;
    }
}
