package ru.sultanyarov.configurator.domain.model;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ComponentType(Long id, Long domainId, String name, String code, String description, Integer orderIndex,
                            Domain domain,
                            LocalDateTime createdAt) {
}
