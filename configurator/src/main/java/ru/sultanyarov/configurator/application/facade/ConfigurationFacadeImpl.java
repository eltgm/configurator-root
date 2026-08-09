package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ModelConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.application.mapper.ConfigurationMapper;
import ru.sultanyarov.configurator.application.service.ConfigurationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationFacadeImpl implements ConfigurationFacade {
  private final ConfigurationService configurationService;
  private final ConfigurationMapper configurationMapper;

  @Override
  public ModelConfiguration create(Long domainId, CreateConfigurationRequest request) {
    log.info("Creating configuration in domain {}", domainId);
    return configurationMapper.toDto(
        configurationService.create(domainId, configurationMapper.toDomain(request)));
  }

  @Override
  public ModelConfiguration update(Long id, UpdateConfigurationRequest request) {
    log.info("Updating configuration {}", id);
    return configurationMapper.toDto(
        configurationService.update(id, configurationMapper.toDomain(request)));
  }

  @Override
  public void delete(Long id) {
    log.info("Deleting configuration {}", id);
    configurationService.delete(id);
  }

  @Override
  public ConfigurationPage getPage(Long domainId, Integer page, Integer size) {
    log.info("Getting configurations in domain {}, page {}, size {}", domainId, page, size);
    return configurationMapper.toDto(configurationService.getPage(domainId, page, size));
  }

  @Override
  public ModelConfiguration getById(Long id) {
    log.info("Getting configuration {}", id);
    return configurationMapper.toDto(configurationService.getById(id));
  }

  @Override
  public ConfigurationExport export(Long id) {
    log.info("Exporting configuration {}", id);
    return configurationMapper.toDto(configurationService.export(id));
  }
}
