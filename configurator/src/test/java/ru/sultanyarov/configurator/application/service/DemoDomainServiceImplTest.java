package ru.sultanyarov.configurator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CurrentUserProvider;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.Domain;

@ExtendWith(MockitoExtension.class)
class DemoDomainServiceImplTest {
  @Mock private DomainService domainService;
  @Mock private ComponentTypeService componentTypeService;
  @Mock private AttributeService attributeService;
  @Mock private ComponentService componentService;
  @Mock private CompatibilityService compatibilityService;
  @Mock private CompatibilityRuleService compatibilityRuleService;
  @Mock private ConfigurationService configurationService;
  @Mock private CurrentUserProvider currentUserProvider;

  @InjectMocks private DemoDomainServiceImpl service;

  @BeforeEach
  void setUp() {
    AtomicLong typeIds = new AtomicLong(10);
    AtomicLong attributeIds = new AtomicLong(100);
    AtomicLong componentIds = new AtomicLong(1000);

    when(currentUserProvider.getCurrentUserId()).thenReturn(42L);
    when(domainService.create(any()))
        .thenAnswer(
            invocation -> {
              Domain source = invocation.getArgument(0);
              return Domain.builder()
                  .id(1L)
                  .name(source.name())
                  .description(source.description())
                  .createdByUserId(source.createdByUserId())
                  .componentTypes(List.of())
                  .build();
            });
    when(componentTypeService.create(any()))
        .thenAnswer(invocation -> savedType(invocation.getArgument(0), typeIds.getAndIncrement()));
    when(attributeService.create(any()))
        .thenAnswer(
            invocation ->
                savedAttribute(invocation.getArgument(0), attributeIds.getAndIncrement()));
    when(componentService.create(any()))
        .thenAnswer(
            invocation -> {
              Component component = invocation.getArgument(0);
              component.setId(componentIds.getAndIncrement());
              return component;
            });
    when(compatibilityService.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(compatibilityRuleService.create(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(configurationService.create(any(), any()))
        .thenReturn(Configuration.builder().id(5000L).build());
  }

  @Test
  void createDemoDomain_shouldCreateCompletePcDataset() throws NoSuchMethodException {
    Domain result = service.createDemoDomain();

    assertThat(result.id()).isEqualTo(1L);
    assertThat(result.name()).isEqualTo(DemoDomainServiceImpl.DEMO_DOMAIN_NAME);
    assertThat(result.createdByUserId()).isEqualTo(42L);
    assertThat(
            DemoDomainServiceImpl.class
                .getMethod("createDemoDomain")
                .isAnnotationPresent(Transactional.class))
        .isTrue();

    ArgumentCaptor<Domain> domainCaptor = ArgumentCaptor.forClass(Domain.class);
    verify(domainService).create(domainCaptor.capture());
    assertThat(domainCaptor.getValue().createdByUserId()).isEqualTo(42L);

    ArgumentCaptor<ComponentType> typeCaptor = ArgumentCaptor.forClass(ComponentType.class);
    verify(componentTypeService, times(6)).create(typeCaptor.capture());
    assertThat(typeCaptor.getAllValues())
        .extracting(ComponentType::code)
        .containsExactly("CPU", "MOTHERBOARD", "MEMORY", "GPU", "PSU", "CASE");
    assertThat(typeCaptor.getAllValues())
        .extracting(ComponentType::orderIndex)
        .containsExactly(0, 1, 2, 3, 4, 5);

    ArgumentCaptor<AttributeDefinition> attributeCaptor =
        ArgumentCaptor.forClass(AttributeDefinition.class);
    verify(attributeService, times(12)).create(attributeCaptor.capture());
    assertThat(attributeCaptor.getAllValues())
        .allSatisfy(
            attribute -> {
              assertThat(attribute.isRequired()).isTrue();
              assertThat(attribute.orderIndex()).isNotNegative();
            });

    ArgumentCaptor<Component> componentCaptor = ArgumentCaptor.forClass(Component.class);
    verify(componentService, times(12)).create(componentCaptor.capture());
    assertThat(componentCaptor.getAllValues())
        .extracting(Component::getName)
        .contains(
            "Ryzen 5 7600",
            "Core i5-14600K",
            "GeForce RTX 4070 SUPER",
            "Radeon RX 7800 XT",
            "Pop Air",
            "MasterBox Q300L Compact");
    assertThat(componentCaptor.getAllValues()).allSatisfy(this::assertActiveComponent);

    ArgumentCaptor<CompatibilityRuleSet> ruleCaptor =
        ArgumentCaptor.forClass(CompatibilityRuleSet.class);
    verify(compatibilityRuleService, times(6)).create(ruleCaptor.capture());
    assertThat(ruleCaptor.getAllValues())
        .extracting(rule -> rule.conditions().getFirst().operator())
        .containsExactly(
            CompatibilityRuleOperator.EQUALS,
            CompatibilityRuleOperator.EQUALS,
            CompatibilityRuleOperator.EQUALS,
            CompatibilityRuleOperator.LTE,
            CompatibilityRuleOperator.LTE,
            CompatibilityRuleOperator.LTE);
    assertThat(ruleCaptor.getAllValues()).allSatisfy(rule -> assertThat(rule.enabled()).isTrue());

    ArgumentCaptor<CompatibilityLink> linkCaptor = ArgumentCaptor.forClass(CompatibilityLink.class);
    verify(compatibilityService, times(2)).create(linkCaptor.capture());
    assertThat(linkCaptor.getAllValues())
        .allSatisfy(
            link -> {
              assertThat(link.domainId()).isEqualTo(1L);
              assertThat(link.componentAId()).isNotEqualTo(link.componentBId());
            });

    ArgumentCaptor<ConfigurationDraft> draftCaptor =
        ArgumentCaptor.forClass(ConfigurationDraft.class);
    verify(configurationService).create(org.mockito.ArgumentMatchers.eq(1L), draftCaptor.capture());
    assertThat(draftCaptor.getValue().name())
        .isEqualTo(DemoDomainServiceImpl.DEMO_CONFIGURATION_NAME);
    assertThat(draftCaptor.getValue().componentIds()).hasSize(6).doesNotHaveDuplicates();
  }

  @Test
  void createDemoDomain_shouldStopAndPropagateFailure() {
    RuntimeException failure = new RuntimeException("configuration creation failed");
    when(configurationService.create(any(), any())).thenThrow(failure);

    assertThatThrownBy(service::createDemoDomain).isSameAs(failure);

    verify(componentService, times(12)).create(any());
    verify(compatibilityRuleService, times(6)).create(any());
    verify(configurationService).create(any(), any());
  }

  private void assertActiveComponent(Component component) {
    assertThat(component.getArchived()).isFalse();
    assertThat(component.getAttributes()).isNotEmpty();
    assertThat(component.getImages()).isEmpty();
  }

  private static ComponentType savedType(ComponentType source, Long id) {
    return ComponentType.builder()
        .id(id)
        .domainId(source.domainId())
        .name(source.name())
        .code(source.code())
        .description(source.description())
        .orderIndex(source.orderIndex())
        .build();
  }

  private static AttributeDefinition savedAttribute(AttributeDefinition source, Long id) {
    return AttributeDefinition.builder()
        .id(id)
        .componentTypeId(source.componentTypeId())
        .name(source.name())
        .label(source.label())
        .dataType(source.dataType())
        .enumValues(source.enumValues())
        .isRequired(source.isRequired())
        .orderIndex(source.orderIndex())
        .build();
  }
}
