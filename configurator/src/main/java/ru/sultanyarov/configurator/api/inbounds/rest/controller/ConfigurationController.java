package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.CREATED;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.ConfigurationsApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ModelConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.application.facade.ConfigurationFacade;

@RestController
@RequiredArgsConstructor
public class ConfigurationController implements ConfigurationsApi {
  private final ConfigurationFacade configurationFacade;

  @Override
  public ResponseEntity<ModelConfiguration> domainsIdConfigurationsPost(
      Long id, CreateConfigurationRequest request) {
    return ResponseEntity.status(CREATED).body(configurationFacade.create(id, request));
  }

  @Override
  public ResponseEntity<ConfigurationPage> domainsIdConfigurationsGet(
      Long id, Integer page, Integer size) {
    return ResponseEntity.ok(configurationFacade.getPage(id, page, size));
  }

  @Override
  public ResponseEntity<ModelConfiguration> configurationsIdGet(Long id) {
    return ResponseEntity.ok(configurationFacade.getById(id));
  }

  @Override
  public ResponseEntity<Void> configurationsIdDelete(Long id) {
    configurationFacade.delete(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<ModelConfiguration> configurationsIdPut(
      Long id, UpdateConfigurationRequest updateConfigurationRequest) {
    return ResponseEntity.ok(configurationFacade.update(id, updateConfigurationRequest));
  }

  @Override
  public ResponseEntity<ConfigurationExport> configurationsIdExportJsonGet(Long id) {
    return ResponseEntity.ok()
        .header(CONTENT_DISPOSITION, "attachment; filename=\"configuration-%d.json\"".formatted(id))
        .body(configurationFacade.export(id));
  }
}
