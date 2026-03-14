package ru.sultanyarov.configurator.application.port.out;

import ru.sultanyarov.configurator.domain.model.Component;

import java.util.Optional;

/**
 * Outbound port for persisting and retrieving {@link Component} entities.
 * Defines persistence operations required by the application layer.
 */
public interface ComponentRepository {
    /**
     * Creates a new component.
     *
     * @param componentToCreate the component entity to create
     * @return the created component with generated ID and technical fields, or empty if creation failed
     */
    Optional<Component> createComponent(Component componentToCreate);

    /**
     * Checks whether at least one component exists for the specified component type.
     *
     * @param id the unique identifier of the component type
     * @return {@code true} if at least one component exists for the specified component type, {@code false} otherwise
     */
    boolean hasByComponentTypeId(Long id);
}
