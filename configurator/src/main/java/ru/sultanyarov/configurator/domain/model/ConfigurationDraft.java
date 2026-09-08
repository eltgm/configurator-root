package ru.sultanyarov.configurator.domain.model;

import java.util.Collection;
import java.util.List;

public record ConfigurationDraft(
    String name,
    String description,
    Boolean trackInventory,
    List<ConfigurationComponentItem> components) {

  /**
   * Keeps callers of the pre-quantity domain API source-compatible while treating each identifier
   * as one requested instance. New code must use the canonical constructor with explicit items.
   */
  public ConfigurationDraft(String name, String description, Collection<Long> componentIds) {
    this(
        name,
        description,
        null,
        componentIds == null
            ? null
            : componentIds.stream()
                .map(componentId -> new ConfigurationComponentItem(componentId, 1))
                .toList());
  }

  /** Returns the component models requested by this draft, without their quantities. */
  public List<Long> componentIds() {
    return components == null
        ? null
        : components.stream().map(ConfigurationComponentItem::componentId).toList();
  }
}
