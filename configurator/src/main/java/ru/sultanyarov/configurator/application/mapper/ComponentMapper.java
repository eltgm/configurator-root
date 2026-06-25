package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

    @Mapping(target = "images", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "archived", ignore = true)
    Component toEntity(CreateComponentRequest createComponentRequest);

    ru.sultanyarov.configurator.api.inbounds.rest.dto.Component toDto(Component component);

    /**
     * Converts a domain page of components to its transport-layer representation.
     *
     * @param byPageByDomainId the page of domain components to convert
     * @return the component page DTO containing mapped components and pagination information
     */
    ComponentPage toComponentPageDto(Page<Component> byPageByDomainId);
}
