package ru.sultanyarov.configurator.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponent;
import ru.sultanyarov.configurator.domain.model.Page;

class ConfigurationMapperTest {
  private final ConfigurationMapper mapper = Mappers.getMapper(ConfigurationMapper.class);

  @Test
  void shouldMapCreateRequestToDomainDraft() {
    CreateConfigurationRequest request =
        new CreateConfigurationRequest("Build", List.of(1L)).description("Description");

    var draft = mapper.toDomain(request);

    assertThat(draft.name()).isEqualTo("Build");
    assertThat(draft.description()).isEqualTo("Description");
    assertThat(draft.componentIds()).containsExactly(1L);
  }

  @Test
  void shouldMapUpdateRequestToDomainDraft() {
    UpdateConfigurationRequest request =
        new UpdateConfigurationRequest("Updated", List.of(2L)).description("New description");

    var draft = mapper.toDomain(request);

    assertThat(draft.name()).isEqualTo("Updated");
    assertThat(draft.description()).isEqualTo("New description");
    assertThat(draft.componentIds()).containsExactly(2L);
  }

  @Test
  void shouldMapConfigurationPageAndExport() {
    Configuration configuration = configuration();

    var dto = mapper.toDto(configuration);
    var page = mapper.toDto(new Page<>(List.of(configuration), 0, 10, 1));
    var export =
        mapper.toDto(
            new ru.sultanyarov.configurator.domain.model.ConfigurationExport(
                1, LocalDateTime.now(), configuration));

    assertThat(dto.getId()).isEqualTo(7L);
    assertThat(dto.getComponents())
        .singleElement()
        .satisfies(
            component -> {
              assertThat(component.getComponentTypeName()).isEqualTo("Board");
              assertThat(component.getArchived()).isTrue();
            });
    assertThat(page.getItems()).singleElement().extracting(item -> item.getId()).isEqualTo(7L);
    assertThat(export.getSchemaVersion()).isEqualTo(1);
    assertThat(export.getConfiguration().getId()).isEqualTo(7L);
  }

  private static Configuration configuration() {
    return Configuration.builder()
        .id(7L)
        .domainId(1L)
        .name("Build")
        .createdAt(LocalDateTime.now())
        .components(
            List.of(
                ConfigurationComponent.builder()
                    .id(1L)
                    .name("Board A")
                    .componentTypeId(10L)
                    .componentTypeName("Board")
                    .archived(true)
                    .build()))
        .build();
  }
}
