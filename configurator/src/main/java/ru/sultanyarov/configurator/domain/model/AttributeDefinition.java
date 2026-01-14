package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AttributeDefinition(Long id, Long componentTypeId, String name, String label, DataType dataType,
                                  List<String> enumValues, boolean isRequired, int orderIndex,
                                  LocalDateTime createdAt) {
}
