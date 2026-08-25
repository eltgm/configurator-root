package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorCandidatesResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorCandidatesResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorIntersectionResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

@Mapper(componentModel = "spring")
public interface ConfiguratorMapper {
  ConfiguratorResponse toDto(ConfiguratorResult result);

  ConfiguratorBatchSearchResponse toDto(ConfiguratorBatchResult result);

  ConfiguratorIntersectionResponse toDto(ConfiguratorIntersectionResult result);

  ConfiguratorCandidatesResponse toDto(ConfiguratorCandidatesResult result);
}
