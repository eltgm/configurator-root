package ru.sultanyarov.configurator.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

@ExtendWith(MockitoExtension.class)
class CompatibilityDecisionResolverTest {
  @Mock private CompatibilityRuleEvaluator ruleEvaluator;

  @Test
  void shouldAllowManualPairWhenNoAutomaticRuleApplies() {
    var resolver = resolver(List.of(link(11L, 1L, 2L)), List.of());

    var decision = resolver.resolve(component(1L, 10L), component(2L, 20L));

    assertThat(decision.status()).isEqualTo(PairCompatibilityStatus.ALLOWED);
    assertThat(decision.explanations())
        .singleElement()
        .satisfies(
            explanation -> {
              assertThat(explanation.linkId()).isEqualTo(11L);
              assertThat(explanation.comment()).isEqualTo("manual");
            });
    assertThat(decision.blockingRules()).isEmpty();
  }

  @Test
  void shouldAllowPairWhenAnyAlternativeRuleMatches() {
    Component left = component(1L, 10L);
    Component right = component(2L, 20L);
    CompatibilityRuleSet failed = rule(7L, 10L, 20L);
    CompatibilityRuleSet matched = rule(8L, 10L, 20L);
    when(ruleEvaluator.evaluate(failed, left, right)).thenReturn(Optional.empty());
    when(ruleEvaluator.evaluate(matched, left, right)).thenReturn(Optional.of(match(8L)));
    var resolver = resolver(List.of(), List.of(failed, matched));

    var decision = resolver.resolve(left, right);

    assertThat(decision.status()).isEqualTo(PairCompatibilityStatus.ALLOWED);
    assertThat(decision.explanations())
        .singleElement()
        .satisfies(explanation -> assertThat(explanation.ruleSetId()).isEqualTo(8L));
    assertThat(decision.blockingRules()).isEmpty();
  }

  @Test
  void shouldDenyPairWhenEveryApplicableRuleFailsEvenWithManualLink() {
    Component left = component(1L, 10L);
    Component right = component(2L, 20L);
    CompatibilityRuleSet first = rule(7L, 10L, 20L);
    CompatibilityRuleSet second = rule(8L, 10L, 20L);
    when(ruleEvaluator.evaluate(first, left, right)).thenReturn(Optional.empty());
    when(ruleEvaluator.evaluate(second, left, right)).thenReturn(Optional.empty());
    var resolver = resolver(List.of(link(11L, 1L, 2L)), List.of(first, second));

    var decision = resolver.resolve(left, right);

    assertThat(decision.status()).isEqualTo(PairCompatibilityStatus.DENIED);
    assertThat(decision.explanations()).isEmpty();
    assertThat(decision.blockingRules())
        .extracting(block -> block.ruleSetId())
        .containsExactly(7L, 8L);
  }

  @Test
  void shouldReturnUnknownWhenPairHasNoRelationshipKnowledge() {
    var resolver = resolver(List.of(), List.of());

    var decision = resolver.resolve(component(1L, 10L), component(2L, 20L));

    assertThat(decision.status()).isEqualTo(PairCompatibilityStatus.UNKNOWN);
    assertThat(decision.explanations()).isEmpty();
    assertThat(decision.blockingRules()).isEmpty();
  }

  @Test
  void shouldOrientComponentsWhenLeftComponentHasRuleTypeB() {
    Component left = component(1L, 20L);
    Component right = component(2L, 10L);
    CompatibilityRuleSet rule = rule(7L, 10L, 20L);
    when(ruleEvaluator.evaluate(rule, right, left)).thenReturn(Optional.of(match(7L)));
    var resolver = resolver(List.of(), List.of(rule));

    assertThat(resolver.resolve(left, right).status()).isEqualTo(PairCompatibilityStatus.ALLOWED);
  }

  private CompatibilityDecisionResolver resolver(
      List<CompatibilityLink> links, List<CompatibilityRuleSet> rules) {
    return CompatibilityDecisionResolver.create(links, rules, ruleEvaluator);
  }

  private static Component component(Long id, Long typeId) {
    return Component.builder().id(id).componentTypeId(typeId).build();
  }

  private static CompatibilityLink link(Long id, Long left, Long right) {
    return CompatibilityLink.builder()
        .id(id)
        .componentAId(left)
        .componentBId(right)
        .comment("manual")
        .build();
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

  private static CompatibilityRuleMatch match(Long ruleSetId) {
    return CompatibilityRuleMatch.builder()
        .ruleSetId(ruleSetId)
        .ruleSetName("Rule " + ruleSetId)
        .conditions(List.of())
        .build();
  }
}
