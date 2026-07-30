package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record CompatibleComponentGroup(
        Long componentTypeId,
        String componentTypeName,
        List<CompatibleComponent> components
) {
}
