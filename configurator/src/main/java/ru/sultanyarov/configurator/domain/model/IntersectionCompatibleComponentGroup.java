package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.util.List;

@Builder
public record IntersectionCompatibleComponentGroup(
        Long componentTypeId,
        String componentTypeName,
        List<IntersectionCompatibleComponent> components
) {
}
