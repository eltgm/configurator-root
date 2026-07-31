package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.validator.CompatibilityRuleValidator;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityRuleServiceImplTest {
    @Mock
    private CompatibilityRuleRepository compatibilityRuleRepository;
    @Mock
    private DomainService domainService;
    @Mock
    private ComponentTypeService componentTypeService;
    @Mock
    private CompatibilityRuleValidator compatibilityRuleValidator;
    @InjectMocks
    private CompatibilityRuleServiceImpl service;

    @Test
    void create_shouldNormalizePairConditionsNameAndDefaultOrderIndex() {
        CompatibilityRuleSet requested = ruleSet(
                null,
                "  Numeric rule  ",
                20L,
                10L,
                condition(201L, CompatibilityRuleOperator.GT, 101L, null)
        );
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(componentTypeService.getById(10L)).thenReturn(componentType(10L));
        when(componentTypeService.getById(20L)).thenReturn(componentType(20L));
        when(compatibilityRuleRepository.create(any())).thenAnswer(invocation ->
                Optional.of(invocation.getArgument(0)));

        CompatibilityRuleSet result = service.create(requested);

        assertThat(result.name()).isEqualTo("Numeric rule");
        assertThat(result.componentTypeAId()).isEqualTo(10L);
        assertThat(result.componentTypeBId()).isEqualTo(20L);
        assertThat(result.conditions()).singleElement().satisfies(condition -> {
            assertThat(condition.leftAttributeDefinitionId()).isEqualTo(101L);
            assertThat(condition.operator()).isEqualTo(CompatibilityRuleOperator.LT);
            assertThat(condition.rightAttributeDefinitionId()).isEqualTo(201L);
            assertThat(condition.orderIndex()).isZero();
        });
        verify(compatibilityRuleValidator).validate(
                result,
                componentType(10L),
                componentType(20L)
        );
    }

    @Test
    void create_shouldPreserveExplicitOrderIndex() {
        CompatibilityRuleSet requested = ruleSet(
                null,
                "Rule",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 8)
        );
        stubValidTypes();
        when(compatibilityRuleRepository.create(any())).thenAnswer(invocation ->
                Optional.of(invocation.getArgument(0)));

        assertThat(service.create(requested).conditions())
                .singleElement()
                .extracting(CompatibilityRuleCondition::orderIndex)
                .isEqualTo(8);
    }

    @Test
    void create_shouldRejectSameComponentTypeBeforeLoadingIt() {
        CompatibilityRuleSet requested = ruleSet(
                null,
                "Rule",
                10L,
                10L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 101L, 0)
        );
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());

        assertThatThrownBy(() -> service.create(requested))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Compatibility rule set must connect different component types");

        verifyNoInteractions(componentTypeService, compatibilityRuleValidator, compatibilityRuleRepository);
    }

    @Test
    void create_shouldReturnConflictForDuplicateBusinessKey() {
        CompatibilityRuleSet requested = ruleSet(
                null,
                "Rule",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );
        stubValidTypes();
        when(compatibilityRuleRepository.create(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(requested))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage(
                        "Compatibility rule set 'Rule' already exists for component types 10 and 20 in domain 1"
                );
    }

    @Test
    void getAllByDomainId_shouldValidateDomainAndReturnScopedRules() {
        CompatibilityRuleSet ruleSet = ruleSet(
                7L,
                "Rule",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(compatibilityRuleRepository.getAllByDomainId(1L)).thenReturn(List.of(ruleSet));

        assertThat(service.getAllByDomainId(1L)).containsExactly(ruleSet);
        verify(domainService).getById(1L);
    }

    @Test
    void getByIdAndDomainId_shouldHideRuleFromAnotherDomainScope() {
        when(domainService.getById(2L)).thenReturn(Domain.builder().id(2L).build());
        when(compatibilityRuleRepository.getByIdAndDomainId(7L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByIdAndDomainId(7L, 2L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Compatibility rule set with id 7 not found in domain with id 2");
    }

    @Test
    void updateByIdAndDomainId_shouldValidateUniquenessExcludingCurrentRule() {
        CompatibilityRuleSet existing = ruleSet(
                7L,
                "Old",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );
        CompatibilityRuleSet requested = ruleSet(
                null,
                "New",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.NOT_EQUALS, 201L, null)
        );
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(compatibilityRuleRepository.getByIdAndDomainId(7L, 1L))
                .thenReturn(Optional.of(existing));
        when(componentTypeService.getById(10L)).thenReturn(componentType(10L));
        when(componentTypeService.getById(20L)).thenReturn(componentType(20L));
        when(compatibilityRuleRepository.existsByBusinessKey(1L, 10L, 20L, "New", 7L))
                .thenReturn(false);
        when(compatibilityRuleRepository.updateByIdAndDomainId(any(), any(), any()))
                .thenAnswer(invocation -> Optional.of(invocation.getArgument(2)));

        CompatibilityRuleSet result = service.updateByIdAndDomainId(7L, 1L, requested);

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.conditions()).singleElement()
                .extracting(CompatibilityRuleCondition::orderIndex)
                .isEqualTo(0);
        verify(compatibilityRuleRepository)
                .existsByBusinessKey(1L, 10L, 20L, "New", 7L);
    }

    @Test
    void updateByIdAndDomainId_shouldRejectDuplicateTargetBusinessKey() {
        CompatibilityRuleSet existing = ruleSet(
                7L,
                "Old",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );
        CompatibilityRuleSet requested = ruleSet(
                null,
                "Duplicate",
                10L,
                20L,
                condition(101L, CompatibilityRuleOperator.EQUALS, 201L, 0)
        );
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(compatibilityRuleRepository.getByIdAndDomainId(7L, 1L))
                .thenReturn(Optional.of(existing));
        when(componentTypeService.getById(10L)).thenReturn(componentType(10L));
        when(componentTypeService.getById(20L)).thenReturn(componentType(20L));
        when(compatibilityRuleRepository.existsByBusinessKey(1L, 10L, 20L, "Duplicate", 7L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateByIdAndDomainId(7L, 1L, requested))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(compatibilityRuleRepository, never()).updateByIdAndDomainId(any(), any(), any());
    }

    @Test
    void deleteByIdAndDomainId_shouldDeleteScopedRule() {
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(compatibilityRuleRepository.deleteByIdAndDomainId(7L, 1L)).thenReturn(true);

        service.deleteByIdAndDomainId(7L, 1L);

        verify(compatibilityRuleRepository).deleteByIdAndDomainId(7L, 1L);
    }

    @Test
    void deleteByIdAndDomainId_shouldReturnNotFoundForMissingScopedRule() {
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(compatibilityRuleRepository.deleteByIdAndDomainId(7L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteByIdAndDomainId(7L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Compatibility rule set with id 7 not found in domain with id 1");
    }

    private void stubValidTypes() {
        when(domainService.getById(1L)).thenReturn(Domain.builder().id(1L).build());
        when(componentTypeService.getById(10L)).thenReturn(componentType(10L));
        when(componentTypeService.getById(20L)).thenReturn(componentType(20L));
    }

    private static CompatibilityRuleSet ruleSet(
            Long id,
            String name,
            Long componentTypeAId,
            Long componentTypeBId,
            CompatibilityRuleCondition... conditions
    ) {
        return CompatibilityRuleSet.builder()
                .id(id)
                .domainId(1L)
                .name(name)
                .componentTypeAId(componentTypeAId)
                .componentTypeBId(componentTypeBId)
                .enabled(true)
                .conditions(List.of(conditions))
                .build();
    }

    private static CompatibilityRuleCondition condition(
            Long leftId,
            CompatibilityRuleOperator operator,
            Long rightId,
            Integer orderIndex
    ) {
        return CompatibilityRuleCondition.builder()
                .leftAttributeDefinitionId(leftId)
                .operator(operator)
                .rightAttributeDefinitionId(rightId)
                .orderIndex(orderIndex)
                .build();
    }

    private static ComponentType componentType(Long id) {
        return ComponentType.builder().id(id).domainId(1L).build();
    }
}
