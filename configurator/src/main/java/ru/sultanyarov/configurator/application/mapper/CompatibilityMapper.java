package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

@Mapper(componentModel = "spring")
public interface CompatibilityMapper {

    @Mapping(target = "id", ignore = true)
    CompatibilityLink toEntity(
            Long domainId,
            Long componentAId,
            Long componentBId,
            String comment
    );

    ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink toDto(
            CompatibilityLink compatibilityLink
    );
}
