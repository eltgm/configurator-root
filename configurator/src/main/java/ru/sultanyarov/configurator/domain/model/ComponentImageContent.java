package ru.sultanyarov.configurator.domain.model;

import java.util.Objects;

/** Original component image bytes and the media type reported by object storage. */
public record ComponentImageContent(byte[] content, String contentType) {
  public ComponentImageContent {
    content = Objects.requireNonNull(content, "content must not be null").clone();
    if (contentType == null || contentType.isBlank()) {
      throw new IllegalArgumentException("contentType must not be blank");
    }
  }

  @Override
  public byte[] content() {
    return content.clone();
  }

  public long contentLength() {
    return content.length;
  }
}
