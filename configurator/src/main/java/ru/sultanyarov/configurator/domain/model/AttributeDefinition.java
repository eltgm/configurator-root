package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record AttributeDefinition(Long id, Long componentTypeId, String name, String label, DataType dataType,
                                  Set<String> enumValues, Boolean isRequired, Integer orderIndex,
                                  LocalDateTime createdAt) {
}
