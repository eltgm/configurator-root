package ru.sultanyarov.configurator.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationComponentInput;
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
        new CreateConfigurationRequest("Build")
            .description("Description")
            .components(List.of(new ConfigurationComponentInput(1L, 4)))
            .trackInventory(true);

    var draft = mapper.toDomain(request);

    assertThat(draft.name()).isEqualTo("Build");
    assertThat(draft.description()).isEqualTo("Description");
    assertThat(draft.componentIds()).containsExactly(1L);
    assertThat(draft.components()).singleElement().extracting("quantity").isEqualTo(4);
    assertThat(draft.trackInventory()).isTrue();
  }

  @Test
  void shouldMapUpdateRequestToDomainDraft() {
    UpdateConfigurationRequest request =
        new UpdateConfigurationRequest("Updated")
            .description("New description")
            .components(List.of(new ConfigurationComponentInput(2L, 2)));

    var draft = mapper.toDomain(request);

    assertThat(draft.name()).isEqualTo("Updated");
    assertThat(draft.description()).isEqualTo("New description");
    assertThat(draft.componentIds()).containsExactly(2L);
    assertThat(draft.components()).singleElement().extracting("quantity").isEqualTo(2);
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
    assertThat(dto.getTrackInventory()).isTrue();
    assertThat(dto.getComponents())
        .singleElement()
        .satisfies(
            component -> {
              assertThat(component.getComponentTypeName()).isEqualTo("Board");
              assertThat(component.getArchived()).isTrue();
              assertThat(component.getQuantity()).isEqualTo(3);
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
        .trackInventory(true)
        .components(
            List.of(
                ConfigurationComponent.builder()
                    .id(1L)
                    .name("Board A")
                    .componentTypeId(10L)
                    .componentTypeName("Board")
                    .archived(true)
                    .quantity(3)
                    .build()))
        .build();
  }
}
