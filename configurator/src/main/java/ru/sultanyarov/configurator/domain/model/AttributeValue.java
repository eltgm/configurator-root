package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record AttributeValue(Long id, Long attributeDefinitionId, String name, String label,
                             DataType dataType,
                             String value) {
}
