package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ConfigurationController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SavedConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.application.facade.ConfigurationFacade;

@ExtendWith(MockitoExtension.class)
class ConfigurationControllerTest {
  @Mock private ConfigurationFacade configurationFacade;
  @InjectMocks private ConfigurationController controller;

  @Test
  void shouldCreateConfiguration() {
    CreateConfigurationRequest request = new CreateConfigurationRequest("Build", List.of(1L));
    SavedConfiguration body = configuration(7L);
    when(configurationFacade.create(1L, request)).thenReturn(body);

    var response = controller.postDomainsByIdConfigurations(1L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(body);
    verify(configurationFacade).create(1L, request);
  }

  @Test
  void shouldGetConfigurationPage() {
    ConfigurationPage body = new ConfigurationPage(List.of(), 0, 10, 0);
    when(configurationFacade.getPage(1L, 0, 10)).thenReturn(body);

    var response = controller.getDomainsByIdConfigurations(1L, 0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(body);
  }

  @Test
  void shouldGetConfigurationById() {
    SavedConfiguration body = configuration(7L);
    when(configurationFacade.getById(7L)).thenReturn(body);

    assertThat(controller.getConfigurationsById(7L).getBody()).isSameAs(body);
  }

  @Test
  void shouldFullyUpdateConfiguration() {
    UpdateConfigurationRequest request = new UpdateConfigurationRequest("Updated", List.of(2L));
    SavedConfiguration body = configuration(7L);
    when(configurationFacade.update(7L, request)).thenReturn(body);

    var response = controller.putConfigurationsById(7L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(body);
    verify(configurationFacade).update(7L, request);
  }

  @Test
  void shouldDeleteConfiguration() {
    var response = controller.deleteConfigurationsById(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
    verify(configurationFacade).delete(7L);
  }

  @Test
  void shouldExportConfigurationAsAttachment() {
    ConfigurationExport body = new ConfigurationExport(1, LocalDateTime.now(), configuration(7L));
    when(configurationFacade.export(7L)).thenReturn(body);

    var response = controller.getConfigurationsByIdExportJson(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
        .isEqualTo("attachment; filename=\"configuration-7.json\"");
    assertThat(response.getBody()).isSameAs(body);
  }

  private static SavedConfiguration configuration(Long id) {
    return new SavedConfiguration(id, 1L, "Build", LocalDateTime.now(), List.of());
  }
}
