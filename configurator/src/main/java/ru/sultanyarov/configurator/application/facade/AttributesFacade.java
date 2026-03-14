package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;

import java.util.List;

/**
 * Facade interface for attribute definition management operations.
 * Provides an application boundary for converting transport-layer requests and responses
 * related to {@link AttributeDefinition} entities.
 */
public interface AttributesFacade {
    /**
     * Updates an existing attribute definition based on the provided request.
     *
     * @param id                               the unique identifier of the attribute definition to update
     * @param createAttributeDefinitionRequest the request containing updated attribute definition details
     * @return the updated attribute definition DTO
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if no attribute definition found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another attribute definition with the same name already exists for the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the request validation fails
     */
    AttributeDefinition updateAttribute(Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

    /**
     * Creates a new attribute definition for the specified component type based on the provided request.
     *
     * @param componentTypeId                  the unique identifier of the component type for which the attribute definition will be created
     * @param createAttributeDefinitionRequest the request containing attribute definition details
     * @return the created attribute definition DTO
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the component type does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if an attribute definition with the same name already exists for the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if the data type validation fails (e.g., missing enum values for enum data type)
     */
    AttributeDefinition createAttribute(Long componentTypeId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

    /**
     * Retrieves all attribute definitions belonging to a specific component type.
     *
     * @param componentTypeId the unique identifier of the component type
     * @return a list of attribute definition DTOs belonging to the specified component type
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type does not exist
     */
    List<AttributeDefinition> getAttributesByComponentTypeId(Long componentTypeId);
}
