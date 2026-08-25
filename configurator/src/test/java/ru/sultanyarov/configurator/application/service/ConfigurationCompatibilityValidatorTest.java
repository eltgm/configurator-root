package ru.sultanyarov.configurator.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;

@ExtendWith(MockitoExtension.class)
class ConfigurationCompatibilityValidatorTest {
  @Mock private ConfiguratorRepository configuratorRepository;
  @Mock private CompatibilityRuleRepository compatibilityRuleRepository;
  @Mock private CompatibilityRuleEvaluator compatibilityRuleEvaluator;

  private ConfigurationCompatibilityValidator validator;

  @BeforeEach
  void setUp() {
    validator =
        new ConfigurationCompatibilityValidator(
            configuratorRepository, compatibilityRuleRepository, compatibilityRuleEvaluator);
  }

  @Test
  void shouldAcceptConnectedAssemblyWithoutEveryPairLinked() {
    when(configuratorRepository.getAllManualCompatibilityLinks(1L))
        .thenReturn(List.of(link(1L, 2L), link(2L, 3L)));
    when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of());

    assertThatCode(
            () ->
                validator.validateAssemblyCompatibility(
                    1L, List.of(component(1L, 10L), component(2L, 20L), component(3L, 30L))))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectDisconnectedSubassemblies() {
    when(configuratorRepository.getAllManualCompatibilityLinks(1L))
        .thenReturn(List.of(link(1L, 2L), link(3L, 4L)));
    when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                validator.validateAssemblyCompatibility(
                    1L,
                    List.of(
                        component(1L, 10L),
                        component(2L, 20L),
                        component(3L, 30L),
                        component(4L, 40L))))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("not connected")
        .hasMessageContaining("3");
  }

  @Test
  void shouldRejectBlockingRuleEvenWhenAllowedGraphIsConnected() {
    Component first = component(1L, 10L);
    Component second = component(2L, 20L);
    Component third = component(3L, 30L);
    CompatibilityRuleSet blockingRule = rule(7L, 20L, 30L);
    when(configuratorRepository.getAllManualCompatibilityLinks(1L))
        .thenReturn(List.of(link(1L, 2L), link(1L, 3L), link(2L, 3L)));
    when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of(blockingRule));
    when(compatibilityRuleEvaluator.evaluate(blockingRule, second, third))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(
            () -> validator.validateAssemblyCompatibility(1L, List.of(first, second, third)))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("2 and 3")
        .hasMessageContaining("Rule 7");
  }

  @Test
  void shouldAcceptSingleComponentWithoutLoadingCompatibilityData() {
    assertThatCode(() -> validator.validateAssemblyCompatibility(1L, List.of(component(1L, 10L))))
        .doesNotThrowAnyException();
  }

  private static CompatibilityLink link(Long left, Long right) {
    return CompatibilityLink.builder().componentAId(left).componentBId(right).build();
  }

  private static Component component(Long id, Long typeId) {
    return Component.builder().id(id).componentTypeId(typeId).build();
  }

  private static CompatibilityRuleSet rule(Long id, Long typeA, Long typeB) {
    return CompatibilityRuleSet.builder()
        .id(id)
        .name("Rule " + id)
        .componentTypeAId(typeA)
        .componentTypeBId(typeB)
        .enabled(true)
        .conditions(List.of())
        .build();
  }
}
