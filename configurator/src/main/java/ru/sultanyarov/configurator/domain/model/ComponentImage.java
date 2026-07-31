package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record ComponentImage(Long id, Long componentId, String url, Integer orderIndex) {
}
