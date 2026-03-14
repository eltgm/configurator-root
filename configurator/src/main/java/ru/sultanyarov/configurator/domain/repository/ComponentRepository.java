package ru.sultanyarov.configurator.domain.repository;

import ru.sultanyarov.configurator.domain.model.Component;

import java.util.Optional;

public interface ComponentRepository {
    Optional<Component> createComponent(Component componentToCreate);
}
