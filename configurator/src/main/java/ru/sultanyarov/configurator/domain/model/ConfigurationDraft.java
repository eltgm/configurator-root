package ru.sultanyarov.configurator.domain.model;

import java.util.List;

public record ConfigurationDraft(String name, String description, List<Long> componentIds) {}
