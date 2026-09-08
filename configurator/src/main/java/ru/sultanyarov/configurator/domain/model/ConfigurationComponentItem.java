package ru.sultanyarov.configurator.domain.model;

/** A requested component model and its number of instances in a configuration. */
public record ConfigurationComponentItem(Long componentId, Integer quantity) {}
