package ru.sultanyarov.configurator.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.validator.CompatibilityRuleValidator;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleOperator;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.ComponentType;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityRuleServiceImpl implements CompatibilityRuleService {
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final DomainService domainService;
    private final ComponentTypeService componentTypeService;
    private final CompatibilityRuleValidator compatibilityRuleValidator;

    @Override
    @Transactional
    public CompatibilityRuleSet create(CompatibilityRuleSet ruleSet) {
        CompatibilityRuleSet normalizedRuleSet = normalize(ruleSet);
        log.debug("create compatibility rule set in domain {}", normalizedRuleSet.domainId());
        domainService.getById(normalizedRuleSet.domainId());
        validateDistinctComponentTypes(normalizedRuleSet);
        validate(normalizedRuleSet);

        return compatibilityRuleRepository.create(normalizedRuleSet)
                .orElseThrow(() -> duplicateException(normalizedRuleSet));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompatibilityRuleSet> getAllByDomainId(Long domainId) {
        log.debug("get compatibility rule sets from domain {}", domainId);
        domainService.getById(domainId);
        return compatibilityRuleRepository.getAllByDomainId(domainId);
    }

    @Override
    @Transactional(readOnly = true)
    public CompatibilityRuleSet getByIdAndDomainId(Long ruleSetId, Long domainId) {
        log.debug("get compatibility rule set {} from domain {}", ruleSetId, domainId);
        domainService.getById(domainId);
        return getScopedRuleSet(ruleSetId, domainId);
    }

    @Override
    @Transactional
    public CompatibilityRuleSet updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            CompatibilityRuleSet ruleSet
    ) {
        log.debug("update compatibility rule set {} in domain {}", ruleSetId, domainId);
        domainService.getById(domainId);
        getScopedRuleSet(ruleSetId, domainId);

        CompatibilityRuleSet normalizedRuleSet = normalize(ruleSet);
        if (!domainId.equals(normalizedRuleSet.domainId())) {
            throw new ValidationException("Compatibility rule-set domain cannot be changed");
        }
        validateDistinctComponentTypes(normalizedRuleSet);
        validate(normalizedRuleSet);
        if (compatibilityRuleRepository.existsByBusinessKey(
                domainId,
                normalizedRuleSet.componentTypeAId(),
                normalizedRuleSet.componentTypeBId(),
                normalizedRuleSet.name(),
                ruleSetId
        )) {
            throw duplicateException(normalizedRuleSet);
        }

        return compatibilityRuleRepository.updateByIdAndDomainId(
                        ruleSetId,
                        domainId,
                        normalizedRuleSet
                )
                .orElseThrow(() -> notFoundException(ruleSetId, domainId));
    }

    @Override
    @Transactional
    public void deleteByIdAndDomainId(Long ruleSetId, Long domainId) {
        log.debug("delete compatibility rule set {} from domain {}", ruleSetId, domainId);
        domainService.getById(domainId);
        if (!compatibilityRuleRepository.deleteByIdAndDomainId(ruleSetId, domainId)) {
            throw notFoundException(ruleSetId, domainId);
        }
    }

    private void validate(CompatibilityRuleSet ruleSet) {
        ComponentType componentTypeA = componentTypeService.getById(ruleSet.componentTypeAId());
        ComponentType componentTypeB = componentTypeService.getById(ruleSet.componentTypeBId());
        compatibilityRuleValidator.validate(ruleSet, componentTypeA, componentTypeB);
    }

    private static void validateDistinctComponentTypes(CompatibilityRuleSet ruleSet) {
        if (ruleSet.componentTypeAId().equals(ruleSet.componentTypeBId())) {
            throw new ValidationException("Compatibility rule set must connect different component types");
        }
    }

    private CompatibilityRuleSet getScopedRuleSet(Long ruleSetId, Long domainId) {
        return compatibilityRuleRepository.getByIdAndDomainId(ruleSetId, domainId)
                .orElseThrow(() -> notFoundException(ruleSetId, domainId));
    }

    private static CompatibilityRuleSet normalize(CompatibilityRuleSet ruleSet) {
        if (ruleSet == null
                || ruleSet.componentTypeAId() == null
                || ruleSet.componentTypeBId() == null) {
            throw new ValidationException("Compatibility rule set has missing required fields");
        }
        boolean reversed = ruleSet.componentTypeAId() > ruleSet.componentTypeBId();
        List<CompatibilityRuleCondition> sourceConditions = ruleSet.conditions() == null
                ? List.of()
                : ruleSet.conditions();
        List<CompatibilityRuleCondition> normalizedConditions = java.util.stream.IntStream
                .range(0, sourceConditions.size())
                .mapToObj(index -> normalizeCondition(sourceConditions.get(index), index, reversed))
                .toList();

        return CompatibilityRuleSet.builder()
                .id(ruleSet.id())
                .domainId(ruleSet.domainId())
                .name(ruleSet.name() == null ? null : ruleSet.name().trim())
                .componentTypeAId(reversed ? ruleSet.componentTypeBId() : ruleSet.componentTypeAId())
                .componentTypeBId(reversed ? ruleSet.componentTypeAId() : ruleSet.componentTypeBId())
                .enabled(ruleSet.enabled())
                .conditions(normalizedConditions)
                .createdAt(ruleSet.createdAt())
                .build();
    }

    private static CompatibilityRuleCondition normalizeCondition(
            CompatibilityRuleCondition condition,
            int index,
            boolean reversed
    ) {
        if (condition == null) {
            throw new ValidationException("Compatibility rule condition must not be null");
        }
        return CompatibilityRuleCondition.builder()
                .id(condition.id())
                .ruleSetId(condition.ruleSetId())
                .leftAttributeDefinitionId(reversed
                        ? condition.rightAttributeDefinitionId()
                        : condition.leftAttributeDefinitionId())
                .operator(reversed ? reverse(condition.operator()) : condition.operator())
                .rightAttributeDefinitionId(reversed
                        ? condition.leftAttributeDefinitionId()
                        : condition.rightAttributeDefinitionId())
                .orderIndex(condition.orderIndex() == null ? index : condition.orderIndex())
                .createdAt(condition.createdAt())
                .build();
    }

    private static CompatibilityRuleOperator reverse(CompatibilityRuleOperator operator) {
        if (operator == null) {
            return null;
        }
        return switch (operator) {
            case EQUALS -> CompatibilityRuleOperator.EQUALS;
            case NOT_EQUALS -> CompatibilityRuleOperator.NOT_EQUALS;
            case GT -> CompatibilityRuleOperator.LT;
            case GTE -> CompatibilityRuleOperator.LTE;
            case LT -> CompatibilityRuleOperator.GT;
            case LTE -> CompatibilityRuleOperator.GTE;
        };
    }

    private static EntityAlreadyExistsException duplicateException(CompatibilityRuleSet ruleSet) {
        return new EntityAlreadyExistsException(
                "Compatibility rule set '{}' already exists for component types {} and {} in domain {}",
                ruleSet.name(),
                ruleSet.componentTypeAId(),
                ruleSet.componentTypeBId(),
                ruleSet.domainId()
        );
    }

    private static NotFoundException notFoundException(Long ruleSetId, Long domainId) {
        return new NotFoundException(
                "Compatibility rule set with id {} not found in domain with id {}",
                ruleSetId,
                domainId
        );
    }
}
