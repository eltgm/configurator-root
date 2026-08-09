package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.StoredImage;

/** Outbound port for storing component image binaries in external object storage. */
public interface ComponentImageStorage {
  /**
   * Stores an image under a component-specific object key.
   *
   * @param componentId the component identifier
   * @param image validated image content
   * @return the stored object key
   */
  StoredImage store(Long componentId, ComponentImageUpload image);

  /**
   * Reads original image bytes and media type.
   *
   * @param objectKey object storage key
   * @return original content and media type
   */
  ComponentImageContent read(String objectKey);

  /**
   * Removes a previously stored object during compensating cleanup.
   *
   * @param objectKey object storage key
   */
  void delete(String objectKey);
}
