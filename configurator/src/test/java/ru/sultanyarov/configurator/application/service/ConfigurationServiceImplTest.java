package ru.sultanyarov.configurator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.application.port.out.ConfigurationRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.application.port.out.CurrentUserProvider;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponentItem;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceImplTest {
  @Mock private DomainService domainService;
  @Mock private ComponentService componentService;
  @Mock private ComponentRepository componentRepository;
  @Mock private ConfiguratorRepository configuratorRepository;
  @Mock private ConfigurationRepository configurationRepository;
  @Mock private CurrentUserProvider currentUserProvider;
  @Mock private ConfigurationCompatibilityValidator compatibilityValidator;

  private ConfigurationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ConfigurationServiceImpl(
            domainService,
            componentService,
            componentRepository,
            configuratorRepository,
            configurationRepository,
            currentUserProvider,
            compatibilityValidator);
  }

  @Test
  void shouldNormalizeValidateAndCreateConfiguration() {
    Domain domain = domain();
    List<Component> components = List.of(component(1L, 10L, false), component(2L, 20L, false));
    Configuration persisted = persistedConfiguration(55L);
    when(domainService.getById(1L)).thenReturn(domain);
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(components);
    when(currentUserProvider.getCurrentUserId()).thenReturn(-1L);
    when(configurationRepository.create(any(Configuration.class)))
        .thenReturn(Optional.of(persisted));

    Configuration result =
        service.create(1L, new ConfigurationDraft("  Build  ", "   ", List.of(1L, 2L)));

    assertThat(result).isSameAs(persisted);
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(configurationRepository).create(captor.capture());
    assertThat(captor.getValue().name()).isEqualTo("Build");
    assertThat(captor.getValue().description()).isNull();
    assertThat(captor.getValue().createdByUserId()).isEqualTo(-1L);
    assertThat(captor.getValue().components())
        .extracting("componentTypeName")
        .containsExactly("Board", "Switch");
    verify(compatibilityValidator).validateAssemblyCompatibility(1L, components);
  }

  @Test
  void shouldFullyUpdateConfigurationAndPreserveImmutableMetadata() {
    LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
    Configuration existing =
        Configuration.builder()
            .id(7L)
            .domainId(1L)
            .name("Initial")
            .description("Initial description")
            .createdByUserId(42L)
            .createdAt(createdAt)
            .components(List.of())
            .build();
    List<Component> components = List.of(component(1L, 10L, false), component(2L, 20L, false));
    Configuration persisted = persistedConfiguration(7L);
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, 42L))
        .thenReturn(Optional.of(existing));
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(components);
    when(configurationRepository.update(any(), any(), any(Configuration.class)))
        .thenReturn(Optional.of(persisted));

    Configuration result =
        service.update(7L, new ConfigurationDraft("  Updated  ", "   ", List.of(1L, 2L)));

    assertThat(result).isSameAs(persisted);
    ArgumentCaptor<Configuration> captor = ArgumentCaptor.forClass(Configuration.class);
    verify(configurationRepository)
        .update(
            org.mockito.ArgumentMatchers.eq(7L),
            org.mockito.ArgumentMatchers.eq(42L),
            captor.capture());
    Configuration update = captor.getValue();
    assertThat(update.id()).isEqualTo(7L);
    assertThat(update.domainId()).isEqualTo(1L);
    assertThat(update.name()).isEqualTo("Updated");
    assertThat(update.description()).isNull();
    assertThat(update.createdByUserId()).isEqualTo(42L);
    assertThat(update.createdAt()).isEqualTo(createdAt);
    assertThat(update.components()).extracting("id").containsExactly(1L, 2L);
    verify(compatibilityValidator).validateAssemblyCompatibility(1L, components);
  }

  @Test
  void shouldHideForeignConfigurationBeforeUpdateValidation() {
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(7L, new ConfigurationDraft("Updated", null, List.of(1L))))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");

    verify(domainService, never()).getById(any());
    verify(configurationRepository, never()).update(any(), any(), any());
  }

  @Test
  void shouldStrictlyRejectArchivedComponentDuringUpdate() {
    Configuration existing = persistedConfiguration(7L);
    when(currentUserProvider.getCurrentUserId()).thenReturn(-1L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, -1L))
        .thenReturn(Optional.of(existing));
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(List.of());
    when(componentService.getById(1L)).thenReturn(component(1L, 10L, true));

    assertThatThrownBy(
            () -> service.update(7L, new ConfigurationDraft("Updated", null, List.of(1L))))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("Archived");

    verify(configurationRepository, never()).update(any(), any(), any());
  }

  @Test
  void shouldReportRepositoryFailureDuringUpdate() {
    Configuration existing = persistedConfiguration(7L);
    Component component = component(1L, 10L, false);
    when(currentUserProvider.getCurrentUserId()).thenReturn(-1L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, -1L))
        .thenReturn(Optional.of(existing));
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(List.of(component));
    when(configurationRepository.update(any(), any(), any(Configuration.class)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(7L, new ConfigurationDraft("Updated", null, List.of(1L))))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to update configuration");
  }

  @Test
  void shouldDeleteConfigurationForCurrentUser() {
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, 42L))
        .thenReturn(Optional.of(persistedConfiguration(7L)));
    when(configurationRepository.deleteByIdAndUserId(7L, 42L)).thenReturn(true);

    service.delete(7L);

    verify(configurationRepository).deleteByIdAndUserId(7L, 42L);
  }

  @Test
  void shouldReturnNotFoundWhenConfigurationCannotBeDeleted() {
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserIdForUpdate(7L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");
  }

  @Test
  void shouldRejectDuplicateComponentIdsBeforeLoadingDomain() {
    assertThatThrownBy(
            () -> service.create(1L, new ConfigurationDraft("Build", null, List.of(1L, 1L))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("unique");
    verify(domainService, never()).getById(any());
  }

  @Test
  void shouldRejectArchivedComponent() {
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(List.of());
    when(componentService.getById(1L)).thenReturn(component(1L, 10L, true));

    assertThatThrownBy(() -> service.create(1L, new ConfigurationDraft("Build", null, List.of(1L))))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("Archived");
  }

  @Test
  void shouldRejectComponentFromAnotherDomain() {
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(List.of());
    when(componentService.getById(9L)).thenReturn(component(9L, 90L, false));

    assertThatThrownBy(() -> service.create(1L, new ConfigurationDraft("Build", null, List.of(9L))))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("does not belong");
  }

  @Test
  void shouldAllowDifferentModelsOfSameComponentType() {
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L))
        .thenReturn(List.of(component(1L, 10L, false), component(2L, 10L, false)));

    when(currentUserProvider.getCurrentUserId()).thenReturn(-1L);
    when(configurationRepository.create(any(Configuration.class)))
        .thenReturn(Optional.of(persistedConfiguration(8L)));

    service.create(1L, new ConfigurationDraft("Build", null, List.of(1L, 2L)));

    verify(compatibilityValidator)
        .validateAssemblyCompatibility(
            1L, List.of(component(1L, 10L, false), component(2L, 10L, false)));
  }

  @Test
  void shouldRejectTrackedConfigurationWhenRequestedQuantityExceedsAvailableInventory() {
    Component component = component(1L, 10L, false);
    component.setTotalQuantity(4);
    when(domainService.getById(1L)).thenReturn(domain());
    when(configuratorRepository.getActiveComponents(1L)).thenReturn(List.of(component));
    when(componentRepository.getById(1L)).thenReturn(Optional.of(component));
    when(configurationRepository.findAllocatedQuantitiesByComponentIds(java.util.Set.of(1L)))
        .thenReturn(Map.of(1L, 2));

    assertThatThrownBy(
            () ->
                service.create(
                    1L,
                    new ConfigurationDraft(
                        "Tracked build",
                        null,
                        true,
                        List.of(new ConfigurationComponentItem(1L, 3)))))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("only 2 are available");

    verify(configurationRepository, never()).create(any(Configuration.class));
  }

  @Test
  void shouldUseCurrentUserForReadsAndDefaultPagination() {
    Page<Configuration> page = new Page<>(List.of(), 0, 10, 0);
    when(domainService.getById(1L)).thenReturn(domain());
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findPageByDomainIdAndUserId(1L, 42L, null, 0, 10))
        .thenReturn(page);

    assertThat(service.getPage(1L, null, null, null)).isSameAs(page);
    verify(configurationRepository).findPageByDomainIdAndUserId(1L, 42L, null, 0, 10);
  }

  @Test
  void shouldRejectInvalidPagination() {
    assertThatThrownBy(() -> service.getPage(1L, -1, 10, null))
        .isInstanceOf(ValidationException.class);
    assertThatThrownBy(() -> service.getPage(1L, 0, 101, null))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  void shouldHideForeignConfigurationAsNotFound() {
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");
  }

  @Test
  void shouldCreateVersionedExport() {
    Configuration configuration = persistedConfiguration(7L);
    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(configurationRepository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.of(configuration));
    LocalDateTime before = LocalDateTime.now(java.time.ZoneOffset.UTC);

    var export = service.export(7L);

    assertThat(export.schemaVersion()).isEqualTo(2);
    assertThat(export.configuration()).isSameAs(configuration);
    assertThat(export.exportedAt()).isAfterOrEqualTo(before);
  }

  private static Domain domain() {
    return Domain.builder()
        .id(1L)
        .componentTypes(
            List.of(
                ComponentType.builder().id(10L).name("Board").build(),
                ComponentType.builder().id(20L).name("Switch").build()))
        .build();
  }

  private static Component component(Long id, Long typeId, boolean archived) {
    return Component.builder()
        .id(id)
        .componentTypeId(typeId)
        .name("Component " + id)
        .archived(archived)
        .build();
  }

  private static Configuration persistedConfiguration(Long id) {
    return Configuration.builder()
        .id(id)
        .domainId(1L)
        .name("Build")
        .createdByUserId(-1L)
        .createdAt(LocalDateTime.now())
        .components(List.of())
        .build();
  }
}
