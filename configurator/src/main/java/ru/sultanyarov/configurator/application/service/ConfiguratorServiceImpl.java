package ru.sultanyarov.configurator.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleMatch;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.CompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguratorServiceImpl implements ConfiguratorService {
    private final DomainService domainService;
    private final ComponentService componentService;
    private final ConfiguratorRepository configuratorRepository;
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final CompatibilityRuleEvaluator compatibilityRuleEvaluator;

    @Override
    @Transactional(readOnly = true)
    public ConfiguratorResult getCompatibleComponents(Long domainId, Long baseComponentId) {
        log.debug(
                "get compatible components in domain {} for base component {}",
                domainId,
                baseComponentId
        );
        Domain domain = domainService.getById(domainId);
        Component baseComponent = componentService.getById(baseComponentId);
        validateBaseComponent(domain, baseComponent);

        List<Component> candidates = configuratorRepository.getActiveCandidates(
                domainId,
                baseComponentId
        );
        List<CompatibilityLink> manualLinks =
                configuratorRepository.getManualCompatibilityLinks(domainId, baseComponentId);
        List<CompatibilityRuleSet> automaticRules =
                compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(
                        domainId,
                        baseComponent.getComponentTypeId()
                );

        Map<Long, List<CompatibilityRuleSet>> rulesByCandidateType =
                indexRulesByCandidateType(automaticRules, baseComponent.getComponentTypeId());
        Map<Long, List<CompatibilityExplanation>> manualExplanations =
                indexManualExplanations(manualLinks, baseComponentId);
        Map<Long, List<CompatibleComponent>> compatibleByType = new HashMap<>();

        for (Component candidate : candidates) {
            List<CompatibilityExplanation> explanations = new ArrayList<>(
                    manualExplanations.getOrDefault(candidate.getId(), List.of())
            );
            explanations.addAll(evaluateRules(
                    baseComponent,
                    candidate,
                    rulesByCandidateType.getOrDefault(candidate.getComponentTypeId(), List.of())
            ));
            if (!explanations.isEmpty()) {
                compatibleByType.computeIfAbsent(
                        candidate.getComponentTypeId(),
                        ignored -> new ArrayList<>()
                ).add(toCompatibleComponent(candidate, explanations));
            }
        }

        return ConfiguratorResult.builder()
                .baseComponentId(baseComponentId)
                .compatibleByType(toOrderedGroups(domain, compatibleByType))
                .build();
    }

    private List<CompatibilityExplanation> evaluateRules(
            Component baseComponent,
            Component candidate,
            List<CompatibilityRuleSet> rules
    ) {
        List<CompatibilityExplanation> explanations = new ArrayList<>();
        for (CompatibilityRuleSet rule : rules) {
            boolean baseIsComponentA =
                    baseComponent.getComponentTypeId().equals(rule.componentTypeAId());
            Component componentA = baseIsComponentA ? baseComponent : candidate;
            Component componentB = baseIsComponentA ? candidate : baseComponent;
            compatibilityRuleEvaluator.evaluate(rule, componentA, componentB)
                    .map(ConfiguratorServiceImpl::toAutomaticExplanation)
                    .ifPresent(explanations::add);
        }
        return explanations;
    }

    private static CompatibilityExplanation toAutomaticExplanation(
            CompatibilityRuleMatch match
    ) {
        return CompatibilityExplanation.builder()
                .source(CompatibilityExplanationSource.AUTOMATIC)
                .ruleSetId(match.ruleSetId())
                .ruleSetName(match.ruleSetName())
                .conditions(match.conditions())
                .build();
    }

    private static Map<Long, List<CompatibilityExplanation>> indexManualExplanations(
            List<CompatibilityLink> links,
            Long baseComponentId
    ) {
        Map<Long, List<CompatibilityExplanation>> result = new HashMap<>();
        for (CompatibilityLink link : links) {
            Long candidateId = baseComponentId.equals(link.componentAId())
                    ? link.componentBId()
                    : link.componentAId();
            result.computeIfAbsent(candidateId, ignored -> new ArrayList<>())
                    .add(CompatibilityExplanation.builder()
                            .source(CompatibilityExplanationSource.MANUAL)
                            .linkId(link.id())
                            .comment(link.comment())
                            .build());
        }
        return result;
    }

    private static Map<Long, List<CompatibilityRuleSet>> indexRulesByCandidateType(
            List<CompatibilityRuleSet> rules,
            Long baseComponentTypeId
    ) {
        Map<Long, List<CompatibilityRuleSet>> result = new HashMap<>();
        for (CompatibilityRuleSet rule : rules) {
            Long candidateTypeId = baseComponentTypeId.equals(rule.componentTypeAId())
                    ? rule.componentTypeBId()
                    : rule.componentTypeAId();
            result.computeIfAbsent(candidateTypeId, ignored -> new ArrayList<>()).add(rule);
        }
        return result;
    }

    private static List<CompatibleComponentGroup> toOrderedGroups(
            Domain domain,
            Map<Long, List<CompatibleComponent>> compatibleByType
    ) {
        List<ComponentType> componentTypes = domain.componentTypes() == null
                ? List.of()
                : domain.componentTypes();
        return componentTypes.stream()
                .filter(componentType -> compatibleByType.containsKey(componentType.id()))
                .map(componentType -> CompatibleComponentGroup.builder()
                        .componentTypeId(componentType.id())
                        .componentTypeName(componentType.name())
                        .components(List.copyOf(compatibleByType.get(componentType.id())))
                        .build())
                .toList();
    }

    private static CompatibleComponent toCompatibleComponent(
            Component component,
            List<CompatibilityExplanation> explanations
    ) {
        return CompatibleComponent.builder()
                .id(component.getId())
                .name(component.getName())
                .brand(component.getBrand())
                .componentTypeId(component.getComponentTypeId())
                .explanations(List.copyOf(explanations))
                .build();
    }

    private static void validateBaseComponent(Domain domain, Component baseComponent) {
        boolean belongsToDomain = domain.componentTypes() != null
                && domain.componentTypes().stream()
                .anyMatch(type -> type.id().equals(baseComponent.getComponentTypeId()));
        if (!belongsToDomain) {
            throw new ValidationException(
                    "Component with id {} does not belong to domain with id {}",
                    baseComponent.getId(),
                    domain.id()
            );
        }
        if (Boolean.TRUE.equals(baseComponent.getArchived())) {
            throw new ValidationException(
                    "Archived component with id {} cannot be used as configurator base",
                    baseComponent.getId()
            );
        }
    }
}
