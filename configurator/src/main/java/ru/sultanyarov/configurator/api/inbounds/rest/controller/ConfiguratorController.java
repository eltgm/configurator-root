package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.ConfiguratorApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.facade.ConfiguratorFacade;

@RestController
@RequiredArgsConstructor
public class ConfiguratorController implements ConfiguratorApi {
  private final ConfiguratorFacade configuratorFacade;

  @Override
  public ResponseEntity<ConfiguratorResponse> getDomainsByIdConfiguratorCompatible(
      Long id, Long componentId, Boolean includeTransitive) {
    return ResponseEntity.ok(
        configuratorFacade.getCompatibleComponents(
            id, componentId, Boolean.TRUE.equals(includeTransitive)));
  }

  @Override
  public ResponseEntity<ConfiguratorBatchSearchResponse>
      postDomainsByIdConfiguratorCompatibleSearch(
          Long id, ConfiguratorBatchSearchRequest configuratorBatchSearchRequest) {
    return ResponseEntity.ok(
        configuratorFacade.searchCompatibleComponents(id, configuratorBatchSearchRequest));
  }

  @Override
  public ResponseEntity<ConfiguratorIntersectionResponse>
      postDomainsByIdConfiguratorCompatibleIntersection(
          Long id, ConfiguratorIntersectionRequest configuratorIntersectionRequest) {
    return ResponseEntity.ok(
        configuratorFacade.intersectCompatibleComponents(id, configuratorIntersectionRequest));
  }
}
