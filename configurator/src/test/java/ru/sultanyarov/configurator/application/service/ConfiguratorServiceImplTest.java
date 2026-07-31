package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibilityConditionExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguratorServiceImplTest {
    @Mock
    private DomainService domainService;
    @Mock
    private ComponentService componentService;
    @Mock
    private ConfiguratorRepository configuratorRepository;
    @Mock
    private CompatibilityRuleRepository compatibilityRuleRepository;
    @Mock
    private CompatibilityRuleEvaluator compatibilityRuleEvaluator;
    @InjectMocks
    private ConfiguratorServiceImpl service;

    @Test
    void getCompatibleComponents_shouldReturnUnionOfManualAndAutomaticResultsInDomainTypeOrder() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        Component automatic = component(2L, 20L, "Automatic", false);
        Component manual = component(3L, 30L, "Manual", false);
        Component duplicateSource = component(4L, 20L, "Both", false);
        CompatibilityRuleSet rule = rule(7L, 10L, 20L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L))
                .thenReturn(List.of(automatic, duplicateSource, manual));
        when(configuratorRepository.getManualCompatibilityLinks(1L, 1L))
                .thenReturn(List.of(
                        link(11L, 1L, 3L, "Manual"),
                        link(12L, 1L, 4L, "Both")
                ));
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of(rule));
        when(compatibilityRuleEvaluator.evaluate(rule, base, automatic))
                .thenReturn(Optional.of(match(7L)));
        when(compatibilityRuleEvaluator.evaluate(rule, base, duplicateSource))
                .thenReturn(Optional.of(match(7L)));

        var result = service.getCompatibleComponents(1L, 1L, false);

        assertThat(result.baseComponentId()).isEqualTo(1L);
        assertThat(result.compatibleByType())
                .extracting(group -> group.componentTypeId())
                .containsExactly(30L, 20L);
        assertThat(result.compatibleByType().get(0).components())
                .extracting(component -> component.id())
                .containsExactly(3L);
        assertThat(result.compatibleByType().get(1).components())
                .extracting(component -> component.id())
                .containsExactly(2L, 4L);
        assertThat(result.compatibleByType().get(0).components().getFirst().explanations())
                .singleElement()
                .satisfies(explanation -> {
                    assertThat(explanation.source())
                            .isEqualTo(CompatibilityExplanationSource.MANUAL);
                    assertThat(explanation.linkId()).isEqualTo(11L);
                    assertThat(explanation.comment()).isEqualTo("Manual");
                });
        assertThat(result.compatibleByType().get(1).components().getFirst().explanations())
                .singleElement()
                .satisfies(explanation -> {
                    assertThat(explanation.source())
                            .isEqualTo(CompatibilityExplanationSource.AUTOMATIC);
                    assertThat(explanation.ruleSetId()).isEqualTo(7L);
                    assertThat(explanation.ruleSetName()).isEqualTo("Rule 7");
                    assertThat(explanation.conditions()).hasSize(1);
                });
        assertThat(result.compatibleByType().get(1).components().get(1).explanations())
                .extracting(explanation -> explanation.source())
                .containsExactly(
                        CompatibilityExplanationSource.MANUAL,
                        CompatibilityExplanationSource.AUTOMATIC
                );
    }

    @Test
    void getCompatibleComponents_shouldTreatRuleSetsAsAlternatives() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        Component candidate = component(2L, 20L, "Candidate", false);
        CompatibilityRuleSet first = rule(7L, 10L, 20L);
        CompatibilityRuleSet second = rule(8L, 10L, 20L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of(candidate));
        when(configuratorRepository.getManualCompatibilityLinks(1L, 1L)).thenReturn(List.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of(first, second));
        when(compatibilityRuleEvaluator.evaluate(first, base, candidate))
                .thenReturn(Optional.empty());
        when(compatibilityRuleEvaluator.evaluate(second, base, candidate))
                .thenReturn(Optional.of(match(8L)));

        assertThat(service.getCompatibleComponents(1L, 1L, false).compatibleByType())
                .singleElement()
                .satisfies(group -> {
                    assertThat(group.components())
                            .extracting(component -> component.id())
                            .containsExactly(2L);
                    assertThat(group.components().getFirst().explanations())
                            .singleElement()
                            .satisfies(explanation ->
                                    assertThat(explanation.ruleSetId()).isEqualTo(8L));
                });
    }

    @Test
    void getCompatibleComponents_shouldReturnEveryMatchingAutomaticRuleExplanation() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        Component candidate = component(2L, 20L, "Candidate", false);
        CompatibilityRuleSet first = rule(7L, 10L, 20L);
        CompatibilityRuleSet second = rule(8L, 10L, 20L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of(candidate));
        when(configuratorRepository.getManualCompatibilityLinks(1L, 1L)).thenReturn(List.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of(first, second));
        when(compatibilityRuleEvaluator.evaluate(first, base, candidate))
                .thenReturn(Optional.of(match(7L)));
        when(compatibilityRuleEvaluator.evaluate(second, base, candidate))
                .thenReturn(Optional.of(match(8L)));

        assertThat(service.getCompatibleComponents(1L, 1L, false)
                .compatibleByType().getFirst().components().getFirst().explanations())
                .extracting(explanation -> explanation.ruleSetId())
                .containsExactly(7L, 8L);
    }

    @Test
    void getCompatibleComponents_shouldOrientComponentsForRuleWhereBaseIsTypeB() {
        Domain domain = domain();
        Component base = component(1L, 20L, "Base", false);
        Component candidate = component(2L, 10L, "Candidate", false);
        CompatibilityRuleSet rule = rule(7L, 10L, 20L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of(candidate));
        when(configuratorRepository.getManualCompatibilityLinks(1L, 1L)).thenReturn(List.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 20L))
                .thenReturn(List.of(rule));
        when(compatibilityRuleEvaluator.evaluate(rule, candidate, base))
                .thenReturn(Optional.of(match(7L)));

        assertThat(service.getCompatibleComponents(1L, 1L, false).compatibleByType())
                .singleElement()
                .satisfies(group -> assertThat(group.components())
                        .extracting(component -> component.id())
                        .containsExactly(2L));
        verify(compatibilityRuleEvaluator).evaluate(rule, candidate, base);
    }

    @Test
    void getCompatibleComponents_shouldReturnEmptyGroupsWithoutMatches() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of());
        when(configuratorRepository.getManualCompatibilityLinks(1L, 1L)).thenReturn(List.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of());

        assertThat(service.getCompatibleComponents(1L, 1L, false).compatibleByType()).isEmpty();
    }

    @Test
    void getCompatibleComponents_shouldTraverseManualAndAutomaticEdgesTransitively() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        Component target = component(3L, 30L, "Target", false);
        Component intermediate = component(2L, 20L, "Intermediate", false);
        CompatibilityRuleSet rule = rule(8L, 20L, 30L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L))
                .thenReturn(List.of(target, intermediate));
        when(configuratorRepository.getAllManualCompatibilityLinks(1L))
                .thenReturn(List.of(link(11L, 1L, 2L, "First hop")));
        when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of(rule));
        when(compatibilityRuleEvaluator.evaluate(rule, intermediate, target))
                .thenReturn(Optional.of(match(8L)));

        var result = service.getCompatibleComponents(1L, 1L, true);

        assertThat(result.compatibleByType())
                .extracting(group -> group.componentTypeId())
                .containsExactly(30L, 20L);
        assertThat(result.compatibleByType().getFirst().components().getFirst())
                .satisfies(component -> {
                    assertThat(component.id()).isEqualTo(3L);
                    assertThat(component.explanations()).singleElement()
                            .satisfies(explanation -> {
                                assertThat(explanation.source())
                                        .isEqualTo(CompatibilityExplanationSource.TRANSITIVE);
                                assertThat(explanation.pathComponentIds())
                                        .containsExactly(1L, 2L, 3L);
                            });
                });
        assertThat(result.compatibleByType().get(1).components().getFirst())
                .satisfies(component -> {
                    assertThat(component.id()).isEqualTo(2L);
                    assertThat(component.explanations()).singleElement()
                            .satisfies(explanation -> {
                                assertThat(explanation.source())
                                        .isEqualTo(CompatibilityExplanationSource.MANUAL);
                                assertThat(explanation.linkId()).isEqualTo(11L);
                            });
                });
    }

    @Test
    void getCompatibleComponents_shouldUseDeterministicShortestPathAndHandleCycles() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        Component target = component(3L, 30L, "Target", false);
        Component firstIntermediate = component(2L, 20L, "First", false);
        Component secondIntermediate = component(4L, 20L, "Second", false);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L))
                .thenReturn(List.of(target, firstIntermediate, secondIntermediate));
        when(configuratorRepository.getAllManualCompatibilityLinks(1L))
                .thenReturn(List.of(
                        link(11L, 1L, 2L, null),
                        link(12L, 2L, 3L, null),
                        link(13L, 1L, 4L, null),
                        link(14L, 3L, 4L, null),
                        link(15L, 2L, 4L, null)
                ));
        when(compatibilityRuleRepository.getEnabledByDomainId(1L)).thenReturn(List.of());

        var result = service.getCompatibleComponents(1L, 1L, true);

        assertThat(result.compatibleByType().getFirst().components().getFirst().explanations())
                .singleElement()
                .satisfies(explanation ->
                        assertThat(explanation.pathComponentIds())
                                .containsExactly(1L, 2L, 3L));
        assertThat(result.compatibleByType().get(1).components())
                .allSatisfy(component ->
                        assertThat(component.explanations().getFirst().source())
                                .isEqualTo(CompatibilityExplanationSource.MANUAL));
    }

    @Test
    void getCompatibleComponents_shouldRejectComponentOutsideDomain() {
        Domain domain = domain();
        Component foreign = component(1L, 99L, "Foreign", false);
        when(domainService.getById(1L)).thenReturn(domain);
        when(componentService.getById(1L)).thenReturn(foreign);

        assertThatThrownBy(() -> service.getCompatibleComponents(1L, 1L, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Component with id 1 does not belong to domain with id 1");

        verifyNoInteractions(configuratorRepository, compatibilityRuleRepository, compatibilityRuleEvaluator);
    }

    @Test
    void getCompatibleComponents_shouldRejectArchivedBaseComponent() {
        Domain domain = domain();
        Component archived = component(1L, 10L, "Archived", true);
        when(domainService.getById(1L)).thenReturn(domain);
        when(componentService.getById(1L)).thenReturn(archived);

        assertThatThrownBy(() -> service.getCompatibleComponents(1L, 1L, false))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Archived component with id 1 cannot be used as configurator base");

        verifyNoInteractions(configuratorRepository, compatibilityRuleRepository, compatibilityRuleEvaluator);
    }

    private void stubBase(Domain domain, Component base) {
        when(domainService.getById(1L)).thenReturn(domain);
        when(componentService.getById(1L)).thenReturn(base);
    }

    private static Domain domain() {
        return Domain.builder()
                .id(1L)
                .componentTypes(List.of(
                        componentType(30L, "Third", 1),
                        componentType(10L, "First", 2),
                        componentType(20L, "Second", 3)
                ))
                .build();
    }

    private static ComponentType componentType(Long id, String name, Integer orderIndex) {
        return ComponentType.builder()
                .id(id)
                .domainId(1L)
                .name(name)
                .orderIndex(orderIndex)
                .build();
    }

    private static Component component(
            Long id,
            Long componentTypeId,
            String name,
            boolean archived
    ) {
        return Component.builder()
                .id(id)
                .componentTypeId(componentTypeId)
                .name(name)
                .brand("Brand")
                .archived(archived)
                .attributes(List.of())
                .build();
    }

    private static CompatibilityRuleSet rule(Long id, Long typeA, Long typeB) {
        return CompatibilityRuleSet.builder()
                .id(id)
                .domainId(1L)
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
                .conditions(List.of(CompatibilityConditionExplanation.builder()
                        .leftAttributeDefinitionId(101L)
                        .leftAttributeName("socket")
                        .leftValue("AM5")
                        .operator(CompatibilityRuleOperator.EQUALS)
                        .rightAttributeDefinitionId(201L)
                        .rightAttributeName("socket")
                        .rightValue("AM5")
                        .build()))
                .build();
    }

    private static CompatibilityLink link(
            Long id,
            Long componentAId,
            Long componentBId,
            String comment
    ) {
        return CompatibilityLink.builder()
                .id(id)
                .domainId(1L)
                .componentAId(componentAId)
                .componentBId(componentBId)
                .comment(comment)
                .build();
    }
}
