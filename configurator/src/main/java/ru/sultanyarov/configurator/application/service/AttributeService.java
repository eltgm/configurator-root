package ru.sultanyarov.configurator.application.service;

import java.util.List;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;

/**
 * Service interface for managing {@link AttributeDefinition} entities. Provides application-level
 * business operations for attribute definitions.
 */
public interface AttributeService {
  /**
   * Creates a new attribute definition.
   *
   * @param attributeDefinition the attribute definition entity to create
   * @return the created attribute definition with generated ID and timestamps
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type
   *     does not exist
   * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if an
   *     attribute definition with the same name already exists for the component type
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the data type
   *     validation fails (e.g., missing enum values for enum data type)
   */
  AttributeDefinition create(AttributeDefinition attributeDefinition);

  /** Creates a catalog definition without attaching it to a component type. */
  AttributeDefinition createInDomain(Long domainId, AttributeDefinition attributeDefinition);

  /** Attaches an existing catalog definition or updates its settings for a component type. */
  AttributeDefinition attachToComponentType(
      Long componentTypeId, Long attributeDefinitionId, Boolean isRequired, Integer orderIndex);

  /** Detaches a definition and deletes its values only from components of that type. */
  void detachFromComponentType(Long componentTypeId, Long attributeDefinitionId);

  /**
   * Updates an existing attribute definition.
   *
   * @param id the unique identifier of the attribute definition to update
   * @param attributeDefinition the domain model containing updated attribute definition details
   * @return the updated attribute definition
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no attribute
   *     definition found with the given ID
   * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another
   *     attribute definition with the same name already exists for the component type
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the data type
   *     validation fails
   */
  AttributeDefinition update(Long id, AttributeDefinition attributeDefinition);

  /**
   * Deletes an attribute definition by its unique identifier.
   *
   * @param id the unique identifier of the attribute definition to delete
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no attribute
   *     definition found with the given ID
   */
  void deleteById(Long id);

  /**
   * Retrieves all attribute definitions belonging to a specific component type.
   *
   * @param componentTypeId the unique identifier of the component type
   * @return a list of attribute definitions belonging to the specified component type
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type
   *     does not exist
   */
  List<AttributeDefinition> getByComponentTypeId(Long componentTypeId);

  /** Retrieves the catalog of a domain. */
  List<AttributeDefinition> getByDomainId(Long domainId);

  /** Retrieves IDs of component types linked to a catalog definition. */
  List<Long> getComponentTypeIds(Long attributeDefinitionId);

  /**
   * Retrieves an attribute definition by its unique identifier.
   *
   * @param id the unique identifier of the attribute definition to retrieve
   * @return the attribute definition with the specified ID
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no attribute
   *     definition found with the given ID
   */
  AttributeDefinition getById(Long id);
}
