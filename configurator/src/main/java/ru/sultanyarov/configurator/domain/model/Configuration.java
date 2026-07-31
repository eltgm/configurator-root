package ru.sultanyarov.configurator.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record Configuration(
    Long id,
    Long domainId,
    String name,
    String description,
    Long createdByUserId,
    LocalDateTime createdAt,
    List<ConfigurationComponent> components) {}
