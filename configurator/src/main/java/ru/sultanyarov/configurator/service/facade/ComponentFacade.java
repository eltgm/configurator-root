package ru.sultanyarov.configurator.service.facade;

import ru.sultanyarov.configurator.domain.dto.Component;
import ru.sultanyarov.configurator.domain.dto.CreateComponentRequest;

public interface ComponentFacade {
    Component createComponent(CreateComponentRequest createComponentRequest);
}
