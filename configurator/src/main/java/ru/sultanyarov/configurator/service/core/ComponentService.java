package ru.sultanyarov.configurator.service.core;

import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

public interface ComponentService {
    Component create(Component component);

    Component update(Long id, Component component);

    void deleteById(Long id);

    Component getById(Long id);

    Page<Component> getPage(int page, int pageSize);
}
