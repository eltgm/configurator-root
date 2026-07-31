package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record Domain(Long id, String name, String description, Long createdByUserId, List<ComponentType> componentTypes,
                     LocalDateTime createdAt) {
}
