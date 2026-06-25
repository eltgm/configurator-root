package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

/**
 * Service interface for managing {@link Component} entities.
 * Provides application-level business operations for components.
 */
public interface ComponentService {
    /**
     * Creates a new component.
     *
     * @param component the component entity to create
     * @return the created component with generated ID, persisted attribute values and technical fields
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException            if the component type does not exist
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a component with the same name already exists within the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException          if component validation fails
     * @throws ru.sultanyarov.configurator.domain.exception.BusinessException            if the component could not be created
     */
    Component create(Component component);

    /**
     * Updates an existing component.
     *
     * @param id        the unique identifier of the component to update
     * @param component the component entity with updated values
     * @return the updated component
     */
    Component update(Long id, Component component);

    /**
     * Deletes a component by its unique identifier.
     *
     * @param id the unique identifier of the component to delete
     */
    void deleteById(Long id);

    /**
     * Retrieves a component by its unique identifier.
     *
     * @param id the unique identifier of the component to retrieve
     * @return the component with the specified ID
     */
    Component getById(Long id);

    /**
     * Retrieves a paginated list of components.
     *
     * @param page     the page number (0-based)
     * @param pageSize the number of items per page
     * @return a page containing components and pagination information
     */
    Page<Component> getPage(int page, int pageSize);

    /**
     * Retrieves a paginated list of components belonging to the specified domain.
     *
     * @param domainId        the unique identifier of the domain
     * @param componentTypeId the optional unique identifier of the component type to filter by
     * @param name            the optional component name to filter by
     * @param page            the page number (0-based)
     * @param size            the number of items per page
     * @return a page containing components matching the specified filters and pagination information
     * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException   if no domain found with the given ID
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type does not belong to the specified domain
     */
    Page<Component> getByPageByDomainId(Long domainId, Long componentTypeId, String name, Integer page, Integer size);
}
