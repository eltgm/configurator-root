package ru.sultanyarov.configurator.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.domain.model.BaseComponentCompatibility;
import ru.sultanyarov.configurator.domain.model.CompatibilityBlockingRule;
import ru.sultanyarov.configurator.domain.model.CompatibilityConditionExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.CompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyCandidate;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyPairDecision;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyStatus;
import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorCandidateBaseDecision;
import ru.sultanyarov.configurator.domain.model.ConfiguratorCandidateStatus;
import ru.sultanyarov.configurator.domain.model.ConfiguratorCandidateTypeGroup;
import ru.sultanyarov.configurator.domain.model.ConfiguratorCandidatesResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorIntersectionResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;
import ru.sultanyarov.configurator.domain.model.IntersectionCompatibleComponent;
import ru.sultanyarov.configurator.domain.model.IntersectionCompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

class ConfiguratorMapperTest {
  private final ConfiguratorMapper mapper = Mappers.getMapper(ConfiguratorMapper.class);

  @org.junit.jupiter.api.BeforeEach
  void wireImageMapper() {
    org.springframework.test.util.ReflectionTestUtils.setField(
        mapper, "componentMapper", Mappers.getMapper(ComponentMapper.class));
  }

  @Test
  void toDto_shouldMapNestedCompatibleComponents() {
    ConfiguratorResult result =
        new ConfiguratorResult(
            1L,
            List.of(
                new CompatibleComponentGroup(
                    20L,
                    "Motherboard",
                    List.of(
                        new CompatibleComponent(
                            2L,
                            "Board",
                            "Brand",
                            20L,
                            null,
                            List.of(
                                CompatibilityExplanation.builder()
                                    .source(CompatibilityExplanationSource.MANUAL)
                                    .linkId(11L)
                                    .comment("Manual")
                                    .build(),
                                CompatibilityExplanation.builder()
                                    .source(CompatibilityExplanationSource.AUTOMATIC)
                                    .ruleSetId(7L)
                                    .ruleSetName("Socket")
                                    .conditions(List.of(conditionExplanation()))
                                    .build(),
                                CompatibilityExplanation.builder()
                                    .source(CompatibilityExplanationSource.TRANSITIVE)
                                    .pathComponentIds(List.of(1L, 3L, 2L))
                                    .build()))))));

    var dto = mapper.toDto(result);

    assertThat(dto.getBaseComponentId()).isEqualTo(1L);
    assertThat(dto.getCompatibleByType())
        .singleElement()
        .satisfies(
            group -> {
              assertThat(group.getComponentTypeId()).isEqualTo(20L);
              assertThat(group.getComponentTypeName()).isEqualTo("Motherboard");
              assertThat(group.getComponents())
                  .singleElement()
                  .satisfies(
                      component -> {
                        assertThat(component.getId()).isEqualTo(2L);
                        assertThat(component.getName()).isEqualTo("Board");
                        assertThat(component.getBrand()).isEqualTo("Brand");
                        assertThat(component.getComponentTypeId()).isEqualTo(20L);
                        assertThat(component.getExplanations()).hasSize(3);
                        assertThat(component.getExplanations().getFirst())
                            .satisfies(
                                explanation -> {
                                  assertThat(explanation.getSource().getValue())
                                      .isEqualTo("MANUAL");
                                  assertThat(explanation.getLinkId()).isEqualTo(11L);
                                  assertThat(explanation.getComment()).isEqualTo("Manual");
                                });
                        assertThat(component.getExplanations().get(1))
                            .satisfies(
                                explanation -> {
                                  assertThat(explanation.getSource().getValue())
                                      .isEqualTo("AUTOMATIC");
                                  assertThat(explanation.getRuleSetId()).isEqualTo(7L);
                                  assertThat(explanation.getRuleSetName()).isEqualTo("Socket");
                                  assertThat(explanation.getConditions())
                                      .singleElement()
                                      .satisfies(
                                          condition -> {
                                            assertThat(condition.getLeftAttributeName())
                                                .isEqualTo("socket");
                                            assertThat(condition.getLeftValue()).isEqualTo("AM5");
                                            assertThat(condition.getOperator().getValue())
                                                .isEqualTo("EQUALS");
                                            assertThat(condition.getRightAttributeName())
                                                .isEqualTo("socket");
                                            assertThat(condition.getRightValue()).isEqualTo("AM5");
                                          });
                                });
                        assertThat(component.getExplanations().get(2))
                            .satisfies(
                                explanation -> {
                                  assertThat(explanation.getSource().getValue())
                                      .isEqualTo("TRANSITIVE");
                                  assertThat(explanation.getPathComponentIds())
                                      .containsExactly(1L, 3L, 2L);
                                });
                      });
            });
  }

  @Test
  void toDto_shouldMapBatchResultsInSourceOrder() {
    ConfiguratorBatchResult result =
        new ConfiguratorBatchResult(
            List.of(new ConfiguratorResult(3L, List.of()), new ConfiguratorResult(1L, List.of())));

    var dto = mapper.toDto(result);

    assertThat(dto.getResults())
        .extracting(response -> response.getBaseComponentId())
        .containsExactly(3L, 1L);
  }

  @Test
  void toDto_shouldMapIntersectionEvidenceInBaseComponentOrder() {
    ConfiguratorIntersectionResult result =
        new ConfiguratorIntersectionResult(
            List.of(3L, 1L),
            List.of(
                new IntersectionCompatibleComponentGroup(
                    20L,
                    "Motherboard",
                    List.of(
                        new IntersectionCompatibleComponent(
                            2L,
                            "Board",
                            "Brand",
                            20L,
                            null,
                            List.of(
                                new BaseComponentCompatibility(3L, List.of()),
                                new BaseComponentCompatibility(1L, List.of())))))));

    var dto = mapper.toDto(result);

    assertThat(dto.getComponentIds()).containsExactly(3L, 1L);
    assertThat(dto.getCompatibleByType())
        .singleElement()
        .satisfies(
            group ->
                assertThat(group.getComponents())
                    .singleElement()
                    .satisfies(
                        component ->
                            assertThat(component.getCompatibilityByBase())
                                .extracting(base -> base.getBaseComponentId())
                                .containsExactly(3L, 1L)));
  }

  @Test
  void toDto_shouldMapAssemblyCandidateStatusesAndBlockingRules() {
    ConfiguratorCandidatesResult result =
        new ConfiguratorCandidatesResult(
            List.of(3L, 1L),
            List.of(
                new ConfiguratorCandidateTypeGroup(
                    20L,
                    "Motherboard",
                    List.of(
                        new ConfiguratorAssemblyCandidate(
                            2L,
                            "Board",
                            "Brand",
                            20L,
                            null,
                            ConfiguratorCandidateStatus.BLOCKED,
                            List.of(
                                new ConfiguratorCandidateBaseDecision(
                                    3L,
                                    PairCompatibilityStatus.DENIED,
                                    List.of(),
                                    List.of(new CompatibilityBlockingRule(7L, "Power")))))))),
            ConfiguratorAssemblyStatus.BLOCKED,
            List.of(
                new ConfiguratorAssemblyPairDecision(
                    3L,
                    1L,
                    PairCompatibilityStatus.DENIED,
                    List.of(),
                    List.of(new CompatibilityBlockingRule(7L, "Power")))));

    var dto = mapper.toDto(result);

    assertThat(dto.getComponentIds()).containsExactly(3L, 1L);
    assertThat(dto.getAssemblyStatus().getValue()).isEqualTo("BLOCKED");
    assertThat(dto.getAssemblyDecisions())
        .singleElement()
        .satisfies(decision -> assertThat(decision.getStatus().getValue()).isEqualTo("DENIED"));
    assertThat(dto.getCandidatesByType())
        .singleElement()
        .satisfies(
            group ->
                assertThat(group.getComponents())
                    .singleElement()
                    .satisfies(
                        candidate -> {
                          assertThat(candidate.getStatus().getValue()).isEqualTo("BLOCKED");
                          assertThat(candidate.getCompatibilityByBase())
                              .singleElement()
                              .satisfies(
                                  decision -> {
                                    assertThat(decision.getStatus().getValue()).isEqualTo("DENIED");
                                    assertThat(decision.getBlockingRules())
                                        .singleElement()
                                        .satisfies(
                                            rule -> {
                                              assertThat(rule.getRuleSetId()).isEqualTo(7L);
                                              assertThat(rule.getRuleSetName()).isEqualTo("Power");
                                            });
                                  });
                        }));
  }

  private static CompatibilityConditionExplanation conditionExplanation() {
    return CompatibilityConditionExplanation.builder()
        .leftAttributeDefinitionId(101L)
        .leftAttributeName("socket")
        .leftValue("AM5")
        .operator(CompatibilityRuleOperator.EQUALS)
        .rightAttributeDefinitionId(201L)
        .rightAttributeName("socket")
        .rightValue("AM5")
        .build();
  }
}
