package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.CONFIGURATION;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.CONFIGURATION_COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.DOMAIN;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponent;
import ru.sultanyarov.configurator.domain.model.ConfigurationListFilter;

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
  void shouldSearchAllRowsEscapeWildcardsAndSortWithStableTies() {
    var percent =
        repository
            .create(configuration("Build 100%_ready", List.of(component(100L))))
            .orElseThrow();
    var first = repository.create(configuration("Alpha", List.of(component(100L)))).orElseThrow();
    var second = repository.create(configuration("alpha", List.of(component(100L)))).orElseThrow();
    dslContext
        .batch(
            java.util.stream.IntStream.range(0, 1000)
                .mapToObj(
                    i ->
                        dslContext
                            .insertInto(CONFIGURATION)
                            .set(CONFIGURATION.DOMAIN_ID, 1L)
                            .set(CONFIGURATION.CREATED_BY_USER_ID, -1L)
                            .set(CONFIGURATION.NAME, "Other " + i))
                .toList())
        .execute();
    var literal =
        repository.findPageByDomainIdAndUserId(
            1L, -1L, null, 0, 10, ConfigurationListFilter.of("%_", "name", "asc"));
    assertThat(literal.totalItems()).isEqualTo(1);
    assertThat(literal.items()).extracting(Configuration::id).containsExactly(percent.id());
    var page =
        repository.findPageByDomainIdAndUserId(
            1L, -1L, null, 1, 1, ConfigurationListFilter.of(" ALPHA ", "name", "asc"));
    assertThat(page.totalItems()).isEqualTo(2);
    assertThat(page.items()).extracting(Configuration::id).containsExactly(first.id());
    assertThat(
            repository
                .findPageByDomainIdAndUserId(
                    1L, -1L, null, 0, 1, ConfigurationListFilter.of("alpha", "name", "desc"))
                .items())
        .extracting(Configuration::id)
        .containsExactly(second.id());
    assertThat(
            repository
                .findPageByDomainIdAndUserId(
                    1L, 999L, null, 0, 10, ConfigurationListFilter.of("Alpha", null, null))
                .totalItems())
        .isZero();
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
  void shouldStoreQuantitiesAndFilterByInventoryTracking() {
    Configuration tracked =
        repository
            .create(
                Configuration.builder()
                    .domainId(1L)
                    .name("Tracked build")
                    .createdByUserId(-1L)
                    .trackInventory(true)
                    .components(
                        List.of(ConfigurationComponent.builder().id(100L).quantity(4).build()))
                    .build())
            .orElseThrow();
    repository.create(configuration("Untracked build", List.of(component(200L)))).orElseThrow();

    assertThat(tracked.trackInventory()).isTrue();
    assertThat(tracked.components())
        .singleElement()
        .extracting(ConfigurationComponent::quantity)
        .isEqualTo(4);
    assertThat(tracked.components())
        .singleElement()
        .extracting(ConfigurationComponent::availableQuantity)
        .isEqualTo(6);
    assertThat(
            repository
                .findPageByDomainIdAndUserId(
                    1L, -1L, true, 0, 10, ConfigurationListFilter.of(null, null, null))
                .items())
        .extracting(Configuration::id)
        .containsExactly(tracked.id());
    assertThat(repository.findAllocatedQuantitiesByComponentIds(List.of(100L)))
        .containsEntry(100L, 4);
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

    var page =
        repository.findPageByDomainIdAndUserId(
            1L, -1L, null, 0, 10, ConfigurationListFilter.of(null, null, null));

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

  @Test
  void shouldFullyUpdateOwnedConfigurationAndPreserveImmutableMetadata() {
    Configuration created =
        repository.create(configuration("Initial", List.of(component(100L)))).orElseThrow();

    Configuration updated =
        repository
            .update(
                created.id(),
                -1L,
                Configuration.builder()
                    .name("Updated")
                    .description(null)
                    .components(List.of(component(200L)))
                    .build())
            .orElseThrow();

    assertThat(updated.name()).isEqualTo("Updated");
    assertThat(updated.description()).isNull();
    assertThat(updated.domainId()).isEqualTo(created.domainId());
    assertThat(updated.createdByUserId()).isEqualTo(created.createdByUserId());
    assertThat(updated.createdAt()).isEqualTo(created.createdAt());
    assertThat(updated.components()).extracting(ConfigurationComponent::id).containsExactly(200L);
  }

  @Test
  void shouldNotUpdateConfigurationOwnedByAnotherUser() {
    Configuration created =
        repository.create(configuration("Initial", List.of(component(100L)))).orElseThrow();

    assertThat(
            repository.update(
                created.id(),
                999L,
                Configuration.builder()
                    .name("Foreign update")
                    .description(null)
                    .components(List.of(component(200L)))
                    .build()))
        .isEmpty();

    Configuration unchanged = repository.findByIdAndUserId(created.id(), -1L).orElseThrow();
    assertThat(unchanged.name()).isEqualTo("Initial");
    assertThat(unchanged.description()).isEqualTo("Description");
    assertThat(unchanged.components()).extracting(ConfigurationComponent::id).containsExactly(100L);
    assertThat(
            dslContext
                .selectCount()
                .from(CONFIGURATION)
                .where(CONFIGURATION.ID.eq(created.id()))
                .fetchOne(0, int.class))
        .isEqualTo(1);
  }

  @Test
  void shouldDeleteOwnedConfigurationWithLinksWithoutDeletingCatalogComponents() {
    Configuration deleted =
        repository
            .create(configuration("Deleted", List.of(component(100L), component(200L))))
            .orElseThrow();
    Configuration retained =
        repository.create(configuration("Retained", List.of(component(100L)))).orElseThrow();

    assertThat(repository.deleteByIdAndUserId(deleted.id(), -1L)).isTrue();

    assertThat(repository.findByIdAndUserId(deleted.id(), -1L)).isEmpty();
    assertThat(repository.findByIdAndUserId(retained.id(), -1L)).isPresent();
    assertThat(
            dslContext
                .selectCount()
                .from(CONFIGURATION_COMPONENT)
                .where(CONFIGURATION_COMPONENT.CONFIGURATION_ID.eq(deleted.id()))
                .fetchOne(0, int.class))
        .isZero();
    assertThat(dslContext.fetchCount(COMPONENT)).isEqualTo(2);
  }

  @Test
  void shouldNotDeleteMissingForeignOwnedOrAlreadyDeletedConfiguration() {
    Configuration created =
        repository.create(configuration("Owned", List.of(component(100L)))).orElseThrow();

    assertThat(repository.deleteByIdAndUserId(created.id(), 999L)).isFalse();
    assertThat(repository.findByIdAndUserId(created.id(), -1L)).isPresent();
    assertThat(repository.deleteByIdAndUserId(999999L, -1L)).isFalse();
    assertThat(repository.deleteByIdAndUserId(created.id(), -1L)).isTrue();
    assertThat(repository.deleteByIdAndUserId(created.id(), -1L)).isFalse();
  }

  private void insertComponent(Long id, Long typeId, String name, boolean archived) {
    dslContext
        .insertInto(COMPONENT)
        .set(COMPONENT.ID, id)
        .set(COMPONENT.COMPONENT_TYPE_ID, typeId)
        .set(COMPONENT.NAME, name)
        .set(COMPONENT.ARCHIVED, archived)
        .set(COMPONENT.TOTAL_QUANTITY, 10)
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
    return ConfigurationComponent.builder().id(id).quantity(1).build();
  }
}
