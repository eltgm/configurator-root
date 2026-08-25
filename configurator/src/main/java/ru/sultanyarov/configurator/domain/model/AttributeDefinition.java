package ru.sultanyarov.configurator.domain.model;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Builder;

@Builder
public record AttributeDefinition(
    Long id,
    Long domainId,
    Long componentTypeId,
    String name,
    String label,
    DataType dataType,
    Set<String> enumValues,
    Boolean isRequired,
    Integer orderIndex,
    LocalDateTime createdAt) {

  public AttributeDefinition(
      Long id,
      Long componentTypeId,
      String name,
      String label,
      DataType dataType,
      Set<String> enumValues,
      Boolean isRequired,
      Integer orderIndex,
      LocalDateTime createdAt) {
    this(
        id,
        null,
        componentTypeId,
        name,
        label,
        dataType,
        enumValues,
        isRequired,
        orderIndex,
        createdAt);
  }
}
