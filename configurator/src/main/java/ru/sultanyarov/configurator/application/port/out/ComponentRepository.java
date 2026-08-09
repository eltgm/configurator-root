package ru.sultanyarov.configurator.application.port.out;

import java.util.*;
import ru.sultanyarov.configurator.domain.model.*;

/**
 * Outbound port for persisting and retrieving {@link Component} entities. Defines persistence
 * operations required by the application layer.
 */
public interface ComponentRepository {
  /**
   * Creates a new component.
   *
   * @param componentToCreate the component entity to create
   * @return the created component with generated ID and technical fields, or empty if creation
   *     failed
   */
  Optional<Component> createComponent(Component componentToCreate);

  /**
   * Retrieves a component with its attribute values and images.
   *
   * @param id the unique identifier of the component
   * @return the component, or empty when it does not exist
   */
  Optional<Component> getById(Long id);

  /**
   * Updates editable fields of an existing component.
   *
   * @param id the unique identifier of the component
   * @param component the target editable state
   * @return the updated component, or empty when the update did not affect a row
   */
  Optional<Component> updateComponent(Long id, Component component);

  /**
   * Marks a component as archived.
   *
   * @param id the unique identifier of the component
   * @return {@code true} when a component row was updated
   */
  boolean archiveComponentById(Long id);

  /**
   * Marks a component as active.
   *
   * @param id the unique identifier of the component
   * @return {@code true} when a component row was updated
   */
  boolean restoreComponentById(Long id);

  /**
   * Persists image metadata for a component.
   *
   * @param image image metadata containing component, object key and display order
   * @return the created image with generated identifier, or empty if creation failed
   */
  Optional<ComponentImage> createImage(ComponentImage image);

  /**
   * Retrieves image metadata by its unique identifier.
   *
   * @param id the component image identifier
   * @return image metadata, or empty when it does not exist
   */
  Optional<ComponentImage> getImageById(Long id);

  /**
   * Permanently deletes image metadata.
   *
   * @param id the component image identifier
   * @return {@code true} when a row was deleted
   */
  boolean deleteImageById(Long id);

  /**
   * Replaces image display indexes for the supplied component.
   *
   * @param componentId the component identifier
   * @param orderedImageIds complete image identifiers in the target order
   * @return number of updated rows
   */
  int updateImageOrder(Long componentId, List<Long> orderedImageIds);

  /**
   * Returns the next display order after the maximum existing image order.
   *
   * @param componentId the component identifier
   * @return next display order, starting at zero
   */
  int getNextImageOrderIndex(Long componentId);

  /**
   * Checks whether at least one component exists for the specified component type.
   *
   * @param id the unique identifier of the component type
   * @return {@code true} if at least one component exists for the specified component type, {@code
   *     false} otherwise
   */
  boolean hasByComponentTypeId(Long id);

  /**
   * Retrieves a paginated list of components belonging to the specified domain.
   *
   * @param domainId the unique identifier of the domain
   * @param componentTypeId the optional unique identifier of the component type to filter by
   * @param name the optional component name to filter by
   * @param page the page number (0-based)
   * @param size the number of items per page
   * @return a page containing components matching the specified filters and pagination information
   */
  Page<Component> findPageByDomainIdComponentTypeIdNameArchived(
      Long domainId, Long componentTypeId, String name, Boolean archived, int page, int size);
}
