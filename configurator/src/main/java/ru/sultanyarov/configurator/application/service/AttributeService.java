package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;

import java.util.List;

/**
 * Service interface for managing {@link AttributeDefinition} entities.
 * Provides application-level business operations for attribute definitions.
 */
public interface AttributeService {
    /**
     * Creates a new attribute definition.
     *
     * @param attributeDefinition the attribute definition entity to create
     * @return the created attribute definition with generated ID and timestamps
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the component type does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if an attribute definition with the same name already exists for the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the data type validation fails (e.g., missing enum values for enum data type)
     */
    AttributeDefinition create(AttributeDefinition attributeDefinition);

    /**
     * Updates an existing attribute definition.
     *
     * @param id                               the unique identifier of the attribute definition to update
     * @param createAttributeDefinitionRequest the request containing updated attribute definition details
     * @return the updated attribute definition
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no attribute definition found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another attribute definition with the same name already exists for the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the data type validation fails
     */
    AttributeDefinition update(Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

    /**
     * Deletes an attribute definition by its unique identifier.
     *
     * @param id the unique identifier of the attribute definition to delete
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no attribute definition found with the given ID
     */
    void deleteById(Long id);

    /**
     * Retrieves all attribute definitions belonging to a specific component type.
     *
     * @param componentTypeId the unique identifier of the component type
     * @return a list of attribute definitions belonging to the specified component type
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type does not exist
     */
    List<AttributeDefinition> getByComponentTypeId(Long componentTypeId);

    /**
     * Retrieves an attribute definition by its unique identifier.
     *
     * @param id the unique identifier of the attribute definition to retrieve
     * @return the attribute definition with the specified ID
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no attribute definition found with the given ID
     */
    AttributeDefinition getById(Long id);
}
