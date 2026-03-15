package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

/**
 * Outbound port for persisting {@link AttributeValue} entities.
 * Defines persistence operations required when saving component attribute values.
 */
public interface AttributeValueRepository {
    /**
     * Persists attribute values belonging to the specified component.
     *
     * @param newComponentAttributes the attribute values to persist
     * @param componentId the unique identifier of the component that owns the attribute values
     * @return the created attribute values enriched with persisted metadata
     */
    List<AttributeValue> createAttributeValues(List<AttributeValue> newComponentAttributes, Long componentId);
}
