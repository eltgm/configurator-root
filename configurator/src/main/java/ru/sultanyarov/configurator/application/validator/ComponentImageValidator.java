package ru.sultanyarov.configurator.application.validator;

import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;

/**
 * Validates component image upload rules independently of HTTP transport details.
 */
public interface ComponentImageValidator {
    long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;

    void validate(ComponentImageUpload image, Integer orderIndex);
}
