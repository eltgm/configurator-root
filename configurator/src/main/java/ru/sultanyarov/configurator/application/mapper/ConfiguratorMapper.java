package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

@Mapper(componentModel = "spring")
public interface ConfiguratorMapper {
    ConfiguratorResponse toDto(ConfiguratorResult result);

    ConfiguratorBatchSearchResponse toDto(ConfiguratorBatchResult result);
}
