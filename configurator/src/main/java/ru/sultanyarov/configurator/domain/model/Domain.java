package ru.sultanyarov.configurator.domain.model;

import java.time.OffsetDateTime;

public record Domain(Long id, String name, String description, Long createdByUserId, OffsetDateTime createdAt) {
}
