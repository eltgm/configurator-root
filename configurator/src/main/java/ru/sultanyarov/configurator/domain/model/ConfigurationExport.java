package ru.sultanyarov.configurator.domain.model;

import java.time.LocalDateTime;

public record ConfigurationExport(
    int schemaVersion, LocalDateTime exportedAt, Configuration configuration) {}
