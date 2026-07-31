package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.DOMAIN;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponent;

class ConfigurationRepositoryImplTest extends AbstractJooqRepositoryTest {
  private ConfigurationRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new ConfigurationRepositoryImpl(dslContext);
    dslContext
        .insertInto(DOMAIN)
        .set(DOMAIN.ID, 1L)
        .set(DOMAIN.NAME, "Keyboards")
        .set(DOMAIN.CREATED_BY_USER_ID, -1L)
        .execute();
    dslContext
        .insertInto(COMPONENT_TYPE)
        .set(COMPONENT_TYPE.ID, 10L)
        .set(COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(COMPONENT_TYPE.NAME, "Board")
        .set(COMPONENT_TYPE.ORDER_INDEX, 2)
        .execute();
    dslContext
        .insertInto(COMPONENT_TYPE)
        .set(COMPONENT_TYPE.ID, 20L)
        .set(COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(COMPONENT_TYPE.NAME, "Switch")
        .set(COMPONENT_TYPE.ORDER_INDEX, 1)
        .execute();
    insertComponent(100L, 10L, "Board A", false);
    insertComponent(200L, 20L, "Switch A", false);
  }

  @Test
  void shouldCreateAndReadConfigurationWithDeterministicComponentOrder() {
    Configuration created =
        repository
            .create(configuration("Build", List.of(component(100L), component(200L))))
            .orElseThrow();

    assertThat(created.id()).isNotNull();
    assertThat(created.name()).isEqualTo("Build");
    assertThat(created.createdAt()).isNotNull();
    assertThat(created.components())
        .extracting(ConfigurationComponent::id)
        .containsExactly(200L, 100L);
    assertThat(created.components().get(0).componentTypeName()).isEqualTo("Switch");
    assertThat(repository.findByIdAndUserId(created.id(), 999L)).isEmpty();
  }

  @Test
  void shouldReturnOwnedConfigurationsInNewestFirstOrderAndReflectArchiveState() {
    Configuration first =
        repository.create(configuration("First", List.of(component(100L)))).orElseThrow();
    Configuration second =
        repository.create(configuration("Second", List.of(component(200L)))).orElseThrow();
    dslContext
        .update(COMPONENT)
        .set(COMPONENT.ARCHIVED, true)
        .where(COMPONENT.ID.eq(200L))
        .execute();

    var page = repository.findPageByDomainIdAndUserId(1L, -1L, 0, 10);

    assertThat(page.totalItems()).isEqualTo(2);
    assertThat(page.items()).extracting(Configuration::id).containsExactly(second.id(), first.id());
    assertThat(
            repository
                .findByIdAndUserId(second.id(), -1L)
                .orElseThrow()
                .components()
                .get(0)
                .archived())
        .isTrue();
  }

  private void insertComponent(Long id, Long typeId, String name, boolean archived) {
    dslContext
        .insertInto(COMPONENT)
        .set(COMPONENT.ID, id)
        .set(COMPONENT.COMPONENT_TYPE_ID, typeId)
        .set(COMPONENT.NAME, name)
        .set(COMPONENT.ARCHIVED, archived)
        .execute();
  }

  private static Configuration configuration(String name, List<ConfigurationComponent> components) {
    return Configuration.builder()
        .domainId(1L)
        .name(name)
        .description("Description")
        .createdByUserId(-1L)
        .components(components)
        .build();
  }

  private static ConfigurationComponent component(Long id) {
    return ConfigurationComponent.builder().id(id).build();
  }
}
