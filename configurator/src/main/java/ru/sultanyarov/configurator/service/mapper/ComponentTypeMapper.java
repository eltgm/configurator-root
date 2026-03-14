package ru.sultanyarov.configurator.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.domain.dto.ComponentType;
import ru.sultanyarov.configurator.domain.dto.CreateComponentTypeRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComponentTypeMapper {
    ComponentType toDto(ru.sultanyarov.configurator.domain.model.ComponentType componentType);

    @Mapping(target = "components", ignore = true)
    @Mapping(target = "attributeDefinitions", ignore = true)
    @Mapping(target = "domain", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "domainId", ignore = true)
    ru.sultanyarov.configurator.domain.model.ComponentType toEntity(Long id, CreateComponentTypeRequest createComponentTypeRequest);

    @Mapping(target = "components", ignore = true)
    @Mapping(target = "attributeDefinitions", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "domain", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    ru.sultanyarov.configurator.domain.model.ComponentType toEntityWithDomain(Long domainId, CreateComponentTypeRequest createComponentTypeRequest);

    List<ComponentType> toDtoList(List<ru.sultanyarov.configurator.domain.model.ComponentType> byDomainId);
}
