package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

@Builder
public record ConfigurationComponent(
    Long id,
    String name,
    String brand,
    Long componentTypeId,
    String componentTypeName,
    boolean archived) {}
