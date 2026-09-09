package ru.sultanyarov.configurator.domain.exception;

import java.util.List;

public class InsufficientComponentAvailabilityException extends ConfigurationConflictException {
  private final List<ComponentShortage> shortages;

  public InsufficientComponentAvailabilityException(List<ComponentShortage> shortages) {
    super("Insufficient component availability");
    if (shortages == null || shortages.isEmpty()) {
      throw new IllegalArgumentException("At least one component shortage is required");
    }
    this.shortages = List.copyOf(shortages);
  }

  public List<ComponentShortage> getShortages() {
    return shortages;
  }

  public record ComponentShortage(
      Long componentId, String componentName, int requestedQuantity, int availableQuantity) {}
}
