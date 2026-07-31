package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ConfiguratorController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.facade.ConfiguratorFacade;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguratorControllerTest {
    @Mock
    private ConfiguratorFacade configuratorFacade;
    @InjectMocks
    private ConfiguratorController controller;

    @Test
    void domainsIdConfiguratorCompatibleGet_shouldDelegateToFacade() {
        ConfiguratorResponse responseBody = new ConfiguratorResponse(7L, List.of());
        when(configuratorFacade.getCompatibleComponents(1L, 7L, true)).thenReturn(responseBody);

        var response = controller.domainsIdConfiguratorCompatibleGet(1L, 7L, true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(responseBody);
        verify(configuratorFacade).getCompatibleComponents(1L, 7L, true);
    }

    @Test
    void domainsIdConfiguratorCompatibleGet_shouldDefaultTransitiveFlagToFalse() {
        ConfiguratorResponse responseBody = new ConfiguratorResponse(7L, List.of());
        when(configuratorFacade.getCompatibleComponents(1L, 7L, false))
                .thenReturn(responseBody);

        var response = controller.domainsIdConfiguratorCompatibleGet(1L, 7L, null);

        assertThat(response.getBody()).isSameAs(responseBody);
        verify(configuratorFacade).getCompatibleComponents(1L, 7L, false);
    }
}
