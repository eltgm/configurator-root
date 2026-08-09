package ru.sultanyarov.configurator.application.service;

import java.util.List;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentImage;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.Page;

/**
 * Service interface for managing {@link Component} entities. Provides application-level business
 * operations for components.
 */
public interface ComponentService {
  /**
   * Creates a new component.
   *
   * @param component the component entity to create
   * @return the created component with generated ID, persisted attribute values and technical
   *     fields
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component type
   *     does not exist
   * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a
   *     component with the same name already exists within the component type
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if component
   *     validation fails
   * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if the component could
   *     not be created
   */
  Component create(Component component);

  /**
   * Updates an existing component.
   *
   * @param id the unique identifier of the component to update
   * @param component the component entity with updated values
   * @return the updated component
   */
  Component update(Long id, Component component);

  /**
   * Archives a component without deleting its related data.
   *
   * @param id the unique identifier of the component to archive
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component does
   *     not exist
   * @throws ru.sultanyarov.configurator.domain.exception.BusinessException if the component could
   *     not be archived
   */
  void archiveById(Long id);

  /**
   * Stores and attaches an image to an active component.
   *
   * @param id the component identifier
   * @param image image binary and media type
   * @param orderIndex optional non-negative display order
   * @return the persisted component image
   */
  ComponentImage uploadImage(Long id, ComponentImageUpload image, Integer orderIndex);

  /**
   * Retrieves images attached to a component.
   *
   * @param id the unique identifier of the component
   * @return component images in deterministic display order
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the component does
   *     not exist
   */
  List<ComponentImage> getImagesByComponentId(Long id);

  /**
   * Retrieves original image content by component image identifier.
   *
   * @param id the component image identifier
   * @return original bytes and media type
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if the image metadata
   *     does not exist
   */
  ComponentImageContent getImageContent(Long id);

  /**
   * Permanently deletes an image from an active component and external storage.
   *
   * @param id the component image identifier
   */
  void deleteImage(Long id);

  /**
   * Replaces the complete image order for an active component.
   *
   * @param id the component identifier
   * @param orderedImageIds complete image identifiers in target order
   * @return images with contiguous target order indexes
   */
  List<ComponentImage> reorderImages(Long id, List<Long> orderedImageIds);

  /**
   * Retrieves a component by its unique identifier.
   *
   * @param id the unique identifier of the component to retrieve
   * @return the component with the specified ID
   */
  Component getById(Long id);

  /**
   * Retrieves a paginated list of components belonging to the specified domain.
   *
   * @param domainId the unique identifier of the domain
   * @param componentTypeId the optional unique identifier of the component type to filter by
   * @param name the optional component name to filter by
   * @param page the page number (0-based)
   * @param size the number of items per page
   * @return a page containing components matching the specified filters and pagination information
   * @throws ru.sultanyarov.configurator.domain.exception.NotFoundException if no domain found with
   *     the given ID
   * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type
   *     does not belong to the specified domain
   */
  Page<Component> getByPageByDomainId(
      Long domainId, Long componentTypeId, String name, Integer page, Integer size);
}
