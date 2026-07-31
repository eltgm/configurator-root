package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.ConfiguratorApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.facade.ConfiguratorFacade;

@RestController
@RequiredArgsConstructor
public class ConfiguratorController implements ConfiguratorApi {
    private final ConfiguratorFacade configuratorFacade;

    @Override
    public ResponseEntity<ConfiguratorResponse> domainsIdConfiguratorCompatibleGet(
            Long id,
            Long componentId,
            Boolean includeTransitive
    ) {
        return ResponseEntity.ok(
                configuratorFacade.getCompatibleComponents(
                        id,
                        componentId,
                        Boolean.TRUE.equals(includeTransitive)
                )
        );
    }
}
