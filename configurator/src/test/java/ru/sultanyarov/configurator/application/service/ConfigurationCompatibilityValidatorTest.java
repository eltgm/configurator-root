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
  void shouldAcceptEveryPairConnectedByDirectManualLinks() {
    when(configuratorRepository.getAllManualCompatibilityLinks(1L))
        .thenReturn(List.of(link(1L, 2L), link(1L, 3L), link(2L, 3L)));
    when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of());

    assertThatCode(
            () ->
                validator.validatePairwiseDirectCompatibility(
                    1L, List.of(component(1L, 10L), component(2L, 20L), component(3L, 30L))))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectTransitiveOnlyPair() {
    when(configuratorRepository.getAllManualCompatibilityLinks(1L))
        .thenReturn(List.of(link(1L, 2L), link(2L, 3L)));
    when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of());

    assertThatThrownBy(
            () ->
                validator.validatePairwiseDirectCompatibility(
                    1L, List.of(component(1L, 10L), component(2L, 20L), component(3L, 30L))))
        .isInstanceOf(ConfigurationConflictException.class)
        .hasMessageContaining("1 and 3");
  }

  private static CompatibilityLink link(Long left, Long right) {
    return CompatibilityLink.builder().componentAId(left).componentBId(right).build();
  }

  private static Component component(Long id, Long typeId) {
    return Component.builder().id(id).componentTypeId(typeId).build();
  }
}
