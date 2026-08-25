package ru.sultanyarov.configurator.application.port.out;

import java.util.List;
import ru.sultanyarov.configurator.domain.model.AttributeValue;

/**
 * Outbound port for persisting {@link AttributeValue} entities. Defines persistence operations
 * required when saving component attribute values.
 */
public interface AttributeValueRepository {
  /**
   * Persists attribute values belonging to the specified component.
   *
   * @param newComponentAttributes the attribute values to persist
   * @param componentId the unique identifier of the component that owns the attribute values
   * @return the created attribute values enriched with persisted metadata
   */
  List<AttributeValue> createAttributeValues(
      List<AttributeValue> newComponentAttributes, Long componentId);

  /**
   * Deletes every attribute value belonging to the specified component.
   *
   * @param componentId the unique identifier of the component
   */
  void deleteByComponentId(Long componentId);

  /** Deletes values of one definition only for components of the specified type. */
  void deleteByAttributeDefinitionIdAndComponentTypeId(
      Long attributeDefinitionId, Long componentTypeId);

  /**
   * Checks whether an attribute definition is already used by at least one component.
   *
   * @param attributeDefinitionId the attribute definition identifier
   * @return {@code true} when a persisted value references the definition
   */
  boolean existsByAttributeDefinitionId(Long attributeDefinitionId);
}
