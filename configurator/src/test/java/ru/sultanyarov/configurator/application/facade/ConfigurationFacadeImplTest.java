package ru.sultanyarov.configurator.application.facade;

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
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SavedConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.application.mapper.ConfigurationMapper;
import ru.sultanyarov.configurator.application.service.ConfigurationService;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.Page;

@ExtendWith(MockitoExtension.class)
class ConfigurationFacadeImplTest {
  @Mock private ConfigurationService configurationService;
  @Mock private ConfigurationMapper configurationMapper;
  @InjectMocks private ConfigurationFacadeImpl facade;

  @Test
  void shouldCreateAndMapConfiguration() {
    CreateConfigurationRequest request = new CreateConfigurationRequest("Build", List.of(1L));
    ConfigurationDraft draft = new ConfigurationDraft("Build", null, List.of(1L));
    Configuration configuration = configuration(7L);
    SavedConfiguration response = response(7L);
    when(configurationMapper.toDomain(request)).thenReturn(draft);
    when(configurationService.create(1L, draft)).thenReturn(configuration);
    when(configurationMapper.toDto(configuration)).thenReturn(response);

    assertThat(facade.create(1L, request)).isSameAs(response);
  }

  @Test
  void shouldUpdateAndMapConfiguration() {
    UpdateConfigurationRequest request = new UpdateConfigurationRequest("Updated", List.of(2L));
    ConfigurationDraft draft = new ConfigurationDraft("Updated", null, List.of(2L));
    Configuration configuration = configuration(7L);
    SavedConfiguration response = response(7L);
    when(configurationMapper.toDomain(request)).thenReturn(draft);
    when(configurationService.update(7L, draft)).thenReturn(configuration);
    when(configurationMapper.toDto(configuration)).thenReturn(response);

    assertThat(facade.update(7L, request)).isSameAs(response);
    verify(configurationMapper).toDomain(request);
    verify(configurationService).update(7L, draft);
    verify(configurationMapper).toDto(configuration);
  }

  @Test
  void shouldDeleteConfiguration() {
    facade.delete(7L);

    verify(configurationService).delete(7L);
  }

  @Test
  void shouldGetPageAndConfiguration() {
    Configuration configuration = configuration(7L);
    Page<Configuration> page = new Page<>(List.of(configuration), 0, 10, 1);
    ConfigurationPage pageResponse = new ConfigurationPage(List.of(response(7L)), 0, 10, 1);
    when(configurationService.getPage(1L, 0, 10)).thenReturn(page);
    when(configurationMapper.toDto(page)).thenReturn(pageResponse);
    when(configurationService.getById(7L)).thenReturn(configuration);
    when(configurationMapper.toDto(configuration)).thenReturn(response(7L));

    assertThat(facade.getPage(1L, 0, 10)).isSameAs(pageResponse);
    assertThat(facade.getById(7L).getId()).isEqualTo(7L);
    verify(configurationService).getPage(1L, 0, 10);
    verify(configurationService).getById(7L);
  }

  @Test
  void shouldExportConfiguration() {
    var domainExport =
        new ru.sultanyarov.configurator.domain.model.ConfigurationExport(
            1, LocalDateTime.now(), configuration(7L));
    var dtoExport =
        new ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport(
            1, LocalDateTime.now(), response(7L));
    when(configurationService.export(7L)).thenReturn(domainExport);
    when(configurationMapper.toDto(domainExport)).thenReturn(dtoExport);

    assertThat(facade.export(7L)).isSameAs(dtoExport);
  }

  private static Configuration configuration(Long id) {
    return Configuration.builder()
        .id(id)
        .domainId(1L)
        .name("Build")
        .createdAt(LocalDateTime.now())
        .components(List.of())
        .build();
  }

  private static SavedConfiguration response(Long id) {
    return new SavedConfiguration(id, 1L, "Build", LocalDateTime.now(), List.of());
  }
}
