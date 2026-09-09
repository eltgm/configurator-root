package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record CompatibleComponent(
    Long id,
    String name,
    String brand,
    Long componentTypeId,
    Integer availableQuantity,
    ComponentImage primaryImage,
    List<AttributeValue> attributes,
    List<CompatibilityExplanation> explanations) {}
