package ru.sultanyarov.configurator.domain.model;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ComponentTypeAttribute(
    Long componentTypeId,
    Long attributeDefinitionId,
    Boolean isRequired,
    Integer orderIndex,
    LocalDateTime createdAt) {}
