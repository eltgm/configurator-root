package ru.sultanyarov.configurator.domain.exception;

public class ComponentTotalBelowAllocatedException extends ConfigurationConflictException {
  private final Long componentId;
  private final String componentName;
  private final int totalQuantity;
  private final int allocatedQuantity;

  public ComponentTotalBelowAllocatedException(
      Long componentId, String componentName, int totalQuantity, int allocatedQuantity) {
    super("Component total quantity cannot be lower than allocated quantity");
    this.componentId = componentId;
    this.componentName = componentName;
    this.totalQuantity = totalQuantity;
    this.allocatedQuantity = allocatedQuantity;
  }

  public Long getComponentId() {
    return componentId;
  }

  public String getComponentName() {
    return componentName;
  }

  public int getTotalQuantity() {
    return totalQuantity;
  }

  public int getAllocatedQuantity() {
    return allocatedQuantity;
  }
}
