package ru.sultanyarov.configurator.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.domain.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.domain.model.Component;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "archived", ignore = true)
    Component toEntity(CreateComponentRequest createComponentRequest);

    ru.sultanyarov.configurator.domain.dto.Component toDto(Component component);
}
