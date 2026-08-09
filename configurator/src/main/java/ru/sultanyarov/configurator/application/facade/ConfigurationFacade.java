package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ModelConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;

public interface ConfigurationFacade {
  ModelConfiguration create(Long domainId, CreateConfigurationRequest request);

  ModelConfiguration update(Long id, UpdateConfigurationRequest request);

  void delete(Long id);

  ConfigurationPage getPage(Long domainId, Integer page, Integer size);

  ModelConfiguration getById(Long id);

  ConfigurationExport export(Long id);
}
