package ru.sultanyarov.configurator.application.validator;

import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.util.Map;

/**
 * Validator interface for component creation rules.
 * Encapsulates application-level validation logic for new {@link Component} entities.
 */
public interface ComponentValidator {
    /**
     * Validates a component before creation.
     *
     * @param componentToCreate the component to validate
     * @param componentType the component type the new component belongs to
     * @param componentTypeAttributesMap a lookup map of attribute definitions belonging to the component type
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if a component with the same name already exists within the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if component attributes or values are invalid
     */
    void validateCreation(Component componentToCreate, ComponentType componentType, Map<Long, AttributeDefinition> componentTypeAttributesMap);

    /**
     * Validates a component before a full update.
     *
     * @param componentToUpdate the target editable state
     * @param existingComponent the currently persisted component
     * @param componentType the immutable component type
     * @param componentTypeAttributesMap a lookup map of attribute definitions belonging to the component type
     * @throws ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException if another component with the same name exists within the component type
     * @throws ru.sultanyarov.configurator.domain.exception.ValidationException if the component type is changed or component attributes are invalid
     */
    void validateUpdate(
            Component componentToUpdate,
            Component existingComponent,
            ComponentType componentType,
            Map<Long, AttributeDefinition> componentTypeAttributesMap
    );
}
