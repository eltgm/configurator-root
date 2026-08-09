package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.ConfigurationExport;
import ru.sultanyarov.configurator.domain.model.Page;

public interface ConfigurationService {
  Configuration create(Long domainId, ConfigurationDraft draft);

  Configuration update(Long id, ConfigurationDraft draft);

  Page<Configuration> getPage(Long domainId, Integer page, Integer size);

  Configuration getById(Long id);

  ConfigurationExport export(Long id);
}
