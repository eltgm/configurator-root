package ru.sultanyarov.configurator.service;

import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;

public interface ComponentValidator {
    void validateCreation(Component componentToCreate, ComponentType componentType);
}
