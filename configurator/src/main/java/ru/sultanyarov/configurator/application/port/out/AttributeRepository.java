package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.AttributeDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for persisting and retrieving {@link AttributeDefinition} entities.
 * Defines persistence operations required by the application layer.
 */
public interface AttributeRepository {
    /**
     * Checks if any attribute definitions exist for the specified component type.
     *
     * @param id the unique identifier of the component type
     * @return {@code true} if attribute definitions exist for the component type, {@code false} otherwise
     */
    boolean hasByComponentTypeId(Long id);

    /**
     * Checks if an attribute definition with the specified name exists for the given component type.
     *
     * @param id   the unique identifier of the component type
     * @param name the name to check for existence
     * @return {@code true} if an attribute definition with the given name exists for the component type, {@code false} otherwise
     */
    boolean hasByComponentTypeIdAndName(Long id, String name);

    /**
     * Creates a new attribute definition.
     *
     * @param attributeDefinition the attribute definition entity to create
     * @return the created attribute definition with generated ID, or empty if creation failed
     */
    Optional<AttributeDefinition> createAttributeDefinition(AttributeDefinition attributeDefinition);

    /**
     * Updates an existing attribute definition.
     *
     * @param id                  the unique identifier of the attribute definition to update
     * @param attributeDefinition the attribute definition entity with updated values
     * @return the updated attribute definition, or empty if update failed
     */
    Optional<AttributeDefinition> updateAttribute(Long id, AttributeDefinition attributeDefinition);

    /**
     * Deletes an attribute definition by its unique identifier.
     *
     * @param id the unique identifier of the attribute definition to delete
     */
    void deleteById(Long id);

    /**
     * Checks if an attribute definition with the specified ID exists.
     *
     * @param id the ID to check for existence
     * @return {@code true} if an attribute definition with the given ID exists, {@code false} otherwise
     */
    boolean existsById(Long id);

    /**
     * Retrieves all attribute definitions belonging to a specific component type.
     *
     * @param componentTypeId the unique identifier of the component type
     * @return a list of attribute definitions belonging to the specified component type
     */
    List<AttributeDefinition> getByComponentTypeId(Long componentTypeId);

    /**
     * Retrieves an attribute definition by its unique identifier.
     *
     * @param id the unique identifier of the attribute definition to retrieve
     * @return the attribute definition with the specified ID, or empty if not found
     */
    Optional<AttributeDefinition> getById(Long id);
}
