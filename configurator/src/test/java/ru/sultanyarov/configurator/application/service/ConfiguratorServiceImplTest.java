package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
        when(configuratorRepository.getManuallyCompatibleComponentIds(1L, 1L))
                .thenReturn(Set.of(3L, 4L));
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of(rule));
        when(compatibilityRuleEvaluator.matches(rule, base, automatic)).thenReturn(true);
        when(compatibilityRuleEvaluator.matches(rule, base, duplicateSource)).thenReturn(true);

        var result = service.getCompatibleComponents(1L, 1L);

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
        when(configuratorRepository.getManuallyCompatibleComponentIds(1L, 1L)).thenReturn(Set.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of(first, second));
        when(compatibilityRuleEvaluator.matches(first, base, candidate)).thenReturn(false);
        when(compatibilityRuleEvaluator.matches(second, base, candidate)).thenReturn(true);

        assertThat(service.getCompatibleComponents(1L, 1L).compatibleByType())
                .singleElement()
                .satisfies(group -> assertThat(group.components())
                        .extracting(component -> component.id())
                        .containsExactly(2L));
    }

    @Test
    void getCompatibleComponents_shouldOrientComponentsForRuleWhereBaseIsTypeB() {
        Domain domain = domain();
        Component base = component(1L, 20L, "Base", false);
        Component candidate = component(2L, 10L, "Candidate", false);
        CompatibilityRuleSet rule = rule(7L, 10L, 20L);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of(candidate));
        when(configuratorRepository.getManuallyCompatibleComponentIds(1L, 1L)).thenReturn(Set.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 20L))
                .thenReturn(List.of(rule));
        when(compatibilityRuleEvaluator.matches(rule, candidate, base)).thenReturn(true);

        assertThat(service.getCompatibleComponents(1L, 1L).compatibleByType())
                .singleElement()
                .satisfies(group -> assertThat(group.components())
                        .extracting(component -> component.id())
                        .containsExactly(2L));
        verify(compatibilityRuleEvaluator).matches(rule, candidate, base);
    }

    @Test
    void getCompatibleComponents_shouldReturnEmptyGroupsWithoutMatches() {
        Domain domain = domain();
        Component base = component(1L, 10L, "Base", false);
        stubBase(domain, base);
        when(configuratorRepository.getActiveCandidates(1L, 1L)).thenReturn(List.of());
        when(configuratorRepository.getManuallyCompatibleComponentIds(1L, 1L)).thenReturn(Set.of());
        when(compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(1L, 10L))
                .thenReturn(List.of());

        assertThat(service.getCompatibleComponents(1L, 1L).compatibleByType()).isEmpty();
    }

    @Test
    void getCompatibleComponents_shouldRejectComponentOutsideDomain() {
        Domain domain = domain();
        Component foreign = component(1L, 99L, "Foreign", false);
        when(domainService.getById(1L)).thenReturn(domain);
        when(componentService.getById(1L)).thenReturn(foreign);

        assertThatThrownBy(() -> service.getCompatibleComponents(1L, 1L))
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

        assertThatThrownBy(() -> service.getCompatibleComponents(1L, 1L))
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
                .componentTypeAId(typeA)
                .componentTypeBId(typeB)
                .enabled(true)
                .conditions(List.of())
                .build();
    }
}
