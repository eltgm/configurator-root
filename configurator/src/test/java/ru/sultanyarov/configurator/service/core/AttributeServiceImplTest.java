package ru.sultanyarov.configurator.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.AttributeRepository;
import ru.sultanyarov.configurator.application.port.out.AttributeValueRepository;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeAttributeRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeRepository;
import ru.sultanyarov.configurator.application.port.out.DomainRepository;
import ru.sultanyarov.configurator.application.service.AttributeServiceImpl;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.ComponentTypeAttribute;
import ru.sultanyarov.configurator.domain.model.DataType;

@ExtendWith(MockitoExtension.class)
class AttributeServiceImplTest {
  @Mock private AttributeRepository attributeRepository;
  @Mock private AttributeValueRepository attributeValueRepository;
  @Mock private ComponentTypeRepository componentTypeRepository;
  @Mock private ComponentTypeAttributeRepository componentTypeAttributeRepository;
  @Mock private DomainRepository domainRepository;
  @Mock private CompatibilityRuleRepository compatibilityRuleRepository;
  @InjectMocks private AttributeServiceImpl attributeService;

  @Test
  void create_shouldCreateCatalogDefinitionAndAttachIt() {
    AttributeDefinition request = linkedRequest(10L, "socket", DataType.STRING, Set.of());
    AttributeDefinition stored = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    when(componentTypeRepository.getComponentTypeById(10L)).thenReturn(Optional.of(type(10L, 1L)));
    when(domainRepository.existsById(1L)).thenReturn(true);
    when(attributeRepository.createAttributeDefinition(any())).thenReturn(Optional.of(stored));
    when(componentTypeAttributeRepository.save(any()))
        .thenReturn(Optional.of(link(10L, 101L, true, 2)));

    AttributeDefinition result = attributeService.create(request);

    assertThat(result.id()).isEqualTo(101L);
    assertThat(result.domainId()).isEqualTo(1L);
    assertThat(result.componentTypeId()).isEqualTo(10L);
    assertThat(result.isRequired()).isTrue();
    assertThat(result.orderIndex()).isEqualTo(2);
    verify(attributeRepository).existsByDomainIdAndName(1L, "socket", null);
  }

  @Test
  void create_shouldRejectMissingComponentTypeBeforePersisting() {
    when(componentTypeRepository.getComponentTypeById(10L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> attributeService.create(linkedRequest(10L, "socket", DataType.STRING, Set.of())))
        .isInstanceOf(NotFoundException.class);

    verify(attributeRepository, never()).createAttributeDefinition(any());
  }

  @Test
  void create_shouldRejectDuplicateNameAnywhereInDomain() {
    when(componentTypeRepository.getComponentTypeById(10L)).thenReturn(Optional.of(type(10L, 1L)));
    when(domainRepository.existsById(1L)).thenReturn(true);
    when(attributeRepository.existsByDomainIdAndName(1L, "socket", null)).thenReturn(true);

    assertThatThrownBy(
            () -> attributeService.create(linkedRequest(10L, "socket", DataType.STRING, Set.of())))
        .isInstanceOf(EntityAlreadyExistsException.class);

    verify(attributeRepository, never()).createAttributeDefinition(any());
  }

  @Test
  void createInDomain_shouldValidateEnumValues() {
    when(domainRepository.existsById(1L)).thenReturn(true);

    assertThatThrownBy(
            () ->
                attributeService.createInDomain(
                    1L,
                    AttributeDefinition.builder()
                        .name("layout")
                        .label("Layout")
                        .dataType(DataType.ENUM)
                        .enumValues(Set.of())
                        .build()))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Enum values");
  }

  @Test
  void attach_shouldReuseDefinitionWithIndependentSettings() {
    AttributeDefinition definition = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    when(componentTypeRepository.getComponentTypeById(20L)).thenReturn(Optional.of(type(20L, 1L)));
    when(attributeRepository.getById(101L)).thenReturn(Optional.of(definition));
    when(componentTypeAttributeRepository.save(any()))
        .thenReturn(Optional.of(link(20L, 101L, false, 4)));

    AttributeDefinition result = attributeService.attachToComponentType(20L, 101L, false, 4);

    assertThat(result.componentTypeId()).isEqualTo(20L);
    assertThat(result.isRequired()).isFalse();
    assertThat(result.orderIndex()).isEqualTo(4);
    verify(attributeRepository, never()).createAttributeDefinition(any());
  }

  @Test
  void attach_shouldRejectDefinitionFromAnotherDomain() {
    when(componentTypeRepository.getComponentTypeById(20L)).thenReturn(Optional.of(type(20L, 1L)));
    when(attributeRepository.getById(301L))
        .thenReturn(Optional.of(catalogDefinition(301L, 2L, "socket", DataType.STRING)));

    assertThatThrownBy(() -> attributeService.attachToComponentType(20L, 301L, false, null))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("does not belong");

    verify(componentTypeAttributeRepository, never()).save(any());
  }

  @Test
  void detach_shouldDeleteScopedValuesBeforeLink() {
    when(componentTypeRepository.getComponentTypeById(20L)).thenReturn(Optional.of(type(20L, 1L)));
    when(attributeRepository.getById(101L))
        .thenReturn(Optional.of(catalogDefinition(101L, 1L, "socket", DataType.STRING)));
    when(componentTypeAttributeRepository.exists(20L, 101L)).thenReturn(true);

    attributeService.detachFromComponentType(20L, 101L);

    verify(attributeValueRepository).deleteByAttributeDefinitionIdAndComponentTypeId(101L, 20L);
    verify(componentTypeAttributeRepository).delete(20L, 101L);
  }

  @Test
  void detach_shouldRejectMissingLink() {
    when(componentTypeRepository.getComponentTypeById(20L)).thenReturn(Optional.of(type(20L, 1L)));
    when(attributeRepository.getById(101L))
        .thenReturn(Optional.of(catalogDefinition(101L, 1L, "socket", DataType.STRING)));

    assertThatThrownBy(() -> attributeService.detachFromComponentType(20L, 101L))
        .isInstanceOf(NotFoundException.class);

    verify(attributeValueRepository, never())
        .deleteByAttributeDefinitionIdAndComponentTypeId(anyLong(), anyLong());
  }

  @Test
  void update_shouldPropagateGlobalFieldsAndPreserveIdentity() {
    AttributeDefinition existing = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    AttributeDefinition replacement =
        AttributeDefinition.builder()
            .name("connector")
            .label("Connector")
            .dataType(DataType.STRING)
            .enumValues(Set.of())
            .build();
    when(attributeRepository.getById(101L)).thenReturn(Optional.of(existing));
    when(attributeRepository.updateAttribute(eq(101L), any()))
        .thenAnswer(invocation -> Optional.of(invocation.getArgument(1)));

    AttributeDefinition result = attributeService.update(101L, replacement);

    assertThat(result.id()).isEqualTo(101L);
    assertThat(result.domainId()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo("connector");
    verify(attributeRepository).existsByDomainIdAndName(1L, "connector", 101L);
  }

  @Test
  void update_shouldRejectDataTypeChangeWithPersistedValues() {
    AttributeDefinition existing = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    when(attributeRepository.getById(101L)).thenReturn(Optional.of(existing));
    when(attributeValueRepository.existsByAttributeDefinitionId(101L)).thenReturn(true);

    assertThatThrownBy(
            () ->
                attributeService.update(
                    101L,
                    AttributeDefinition.builder()
                        .name("socket")
                        .label("Socket")
                        .dataType(DataType.NUMBER)
                        .build()))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("persisted values");

    verify(attributeRepository, never()).updateAttribute(anyLong(), any());
  }

  @Test
  void delete_shouldCascadeWhenDefinitionIsNotUsedByRules() {
    when(attributeRepository.existsById(101L)).thenReturn(true);

    attributeService.deleteById(101L);

    verify(attributeRepository).deleteById(101L);
  }

  @Test
  void delete_shouldReturnConflictWhenDefinitionIsUsedByRule() {
    when(attributeRepository.existsById(101L)).thenReturn(true);
    when(compatibilityRuleRepository.hasByAttributeDefinitionId(101L)).thenReturn(true);

    assertThatThrownBy(() -> attributeService.deleteById(101L))
        .isInstanceOf(EntityHasRelatedEntitiesException.class)
        .hasMessageContaining("compatibility rules");

    verify(attributeRepository, never()).deleteById(anyLong());
  }

  @Test
  void getByDomainAndType_shouldValidateScopeAndReturnDefinitions() {
    AttributeDefinition definition = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    when(domainRepository.existsById(1L)).thenReturn(true);
    when(attributeRepository.getByDomainId(1L)).thenReturn(List.of(definition));
    when(componentTypeRepository.getComponentTypeById(10L)).thenReturn(Optional.of(type(10L, 1L)));
    when(attributeRepository.getByComponentTypeId(10L)).thenReturn(List.of(definition));

    assertThat(attributeService.getByDomainId(1L)).containsExactly(definition);
    assertThat(attributeService.getByComponentTypeId(10L)).containsExactly(definition);
  }

  @Test
  void getComponentTypeIds_shouldRequireExistingDefinition() {
    when(attributeRepository.existsById(101L)).thenReturn(true);
    when(componentTypeAttributeRepository.getComponentTypeIdsByAttributeDefinitionId(101L))
        .thenReturn(List.of(10L, 20L));

    assertThat(attributeService.getComponentTypeIds(101L)).containsExactly(10L, 20L);
  }

  @Test
  void catalogCreate_shouldRejectDuplicateWithoutAnyTypeLinks() {
    when(domainRepository.existsById(1L)).thenReturn(true);
    when(attributeRepository.existsByDomainIdAndName(1L, "socket", null)).thenReturn(true);
    assertThatThrownBy(
            () ->
                attributeService.createInDomain(
                    1L, catalogDefinition(null, 1L, "socket", DataType.STRING)))
        .isInstanceOf(EntityAlreadyExistsException.class);
    verify(attributeRepository, never()).createAttributeDefinition(any());
    verify(componentTypeAttributeRepository, never()).save(any());
  }

  @Test
  void update_shouldRejectDuplicateWithoutAnyTypeLinks() {
    when(attributeRepository.getById(101L))
        .thenReturn(Optional.of(catalogDefinition(101L, 1L, "socket", DataType.STRING)));
    when(attributeRepository.existsByDomainIdAndName(1L, "connector", 101L)).thenReturn(true);
    assertThatThrownBy(
            () ->
                attributeService.update(
                    101L, catalogDefinition(null, 1L, "connector", DataType.STRING)))
        .isInstanceOf(EntityAlreadyExistsException.class);
    verify(attributeRepository, never()).updateAttribute(anyLong(), any());
  }

  @Test
  void update_shouldAllowUnchangedNameAndExcludeItsOwnId() {
    var definition = catalogDefinition(101L, 1L, "socket", DataType.STRING);
    when(attributeRepository.getById(101L)).thenReturn(Optional.of(definition));
    when(attributeRepository.updateAttribute(eq(101L), any())).thenReturn(Optional.of(definition));
    assertThat(attributeService.update(101L, definition)).isEqualTo(definition);
    verify(attributeRepository).existsByDomainIdAndName(1L, "socket", 101L);
  }

  private static AttributeDefinition linkedRequest(
      Long componentTypeId, String name, DataType dataType, Set<String> enumValues) {
    return AttributeDefinition.builder()
        .componentTypeId(componentTypeId)
        .name(name)
        .label("Socket")
        .dataType(dataType)
        .enumValues(enumValues)
        .isRequired(true)
        .orderIndex(2)
        .build();
  }

  private static AttributeDefinition catalogDefinition(
      Long id, Long domainId, String name, DataType dataType) {
    return AttributeDefinition.builder()
        .id(id)
        .domainId(domainId)
        .name(name)
        .label("Socket")
        .dataType(dataType)
        .enumValues(Set.of())
        .createdAt(LocalDateTime.of(2026, 8, 25, 12, 0))
        .build();
  }

  private static ComponentType type(Long id, Long domainId) {
    return ComponentType.builder().id(id).domainId(domainId).name("Type " + id).build();
  }

  private static ComponentTypeAttribute link(
      Long componentTypeId, Long attributeDefinitionId, Boolean isRequired, Integer orderIndex) {
    return ComponentTypeAttribute.builder()
        .componentTypeId(componentTypeId)
        .attributeDefinitionId(attributeDefinitionId)
        .isRequired(isRequired)
        .orderIndex(orderIndex)
        .build();
  }
}
