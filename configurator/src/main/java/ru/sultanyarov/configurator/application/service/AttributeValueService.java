package ru.sultanyarov.configurator.application.service;
import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

/**
 * Service interface for managing {@link AttributeValue} entities.
 * Provides application-level operations for persisting component attribute values.
 */
public interface AttributeValueService {
    /**
     * Persists attribute values belonging to the specified component.
     *
     * @param attributeValues the attribute values to persist
     * @param componentId the unique identifier of the component that owns the attribute values
     * @return the created attribute values enriched with persisted metadata
     */
    List<AttributeValue> createAttributeValues(List<AttributeValue> attributeValues, Long componentId);

    /**
     * Replaces all attribute values belonging to the specified component.
     *
     * @param attributeValues the target attribute values
     * @param componentId the unique identifier of the component
     * @return the persisted target attribute values
     */
    List<AttributeValue> replaceAttributeValues(List<AttributeValue> attributeValues, Long componentId);
}
