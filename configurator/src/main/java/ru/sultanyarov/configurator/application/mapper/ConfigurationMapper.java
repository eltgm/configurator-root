package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ModelConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.Page;

@Mapper(componentModel = "spring")
public interface ConfigurationMapper {
  ConfigurationDraft toDomain(CreateConfigurationRequest request);

  ConfigurationDraft toDomain(UpdateConfigurationRequest request);

  ModelConfiguration toDto(ru.sultanyarov.configurator.domain.model.Configuration configuration);

  ConfigurationPage toDto(
      Page<ru.sultanyarov.configurator.domain.model.Configuration> configurations);

  @Mapping(target = "_configuration", source = "configuration")
  ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport toDto(
      ru.sultanyarov.configurator.domain.model.ConfigurationExport export);
}
