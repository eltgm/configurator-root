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
import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguratorServiceImpl implements ConfiguratorService {
    private static final int MAX_BATCH_COMPONENTS = 50;

    private final DomainService domainService;
    private final ComponentService componentService;
    private final ConfiguratorRepository configuratorRepository;
    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final CompatibilityRuleEvaluator compatibilityRuleEvaluator;

    @Override
    @Transactional(readOnly = true)
    public ConfiguratorResult getCompatibleComponents(
            Long domainId,
            Long baseComponentId,
            boolean includeTransitive
    ) {
        log.debug(
                "get compatible components in domain {} for base component {}, include transitive: {}",
                domainId,
                baseComponentId,
                includeTransitive
        );
        Domain domain = domainService.getById(domainId);
        Component baseComponent = componentService.getById(baseComponentId);
        validateBaseComponent(domain, baseComponent);

        List<Component> candidates = configuratorRepository.getActiveCandidates(
                domainId,
                baseComponentId
        );
        Map<Long, List<CompatibleComponent>> compatibleByType = includeTransitive
                ? findTransitiveCompatibility(domainId, baseComponent, candidates)
                : findDirectCompatibility(domainId, baseComponent, candidates);

        return ConfiguratorResult.builder()
                .baseComponentId(baseComponentId)
                .compatibleByType(toOrderedGroups(domain, compatibleByType))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguratorBatchResult searchCompatibleComponents(
            Long domainId,
            List<Long> baseComponentIds,
            boolean includeTransitive
    ) {
        log.debug(
                "search compatible components in domain {} for base components {}, "
                        + "include transitive: {}",
                domainId,
                baseComponentIds,
                includeTransitive
        );
        validateBatchComponentIds(baseComponentIds);
        Domain domain = domainService.getById(domainId);
        List<Component> activeComponents = configuratorRepository.getActiveComponents(domainId);
        Map<Long, Component> activeComponentsById = new HashMap<>();
        for (Component component : activeComponents) {
            activeComponentsById.put(component.getId(), component);
        }
        for (Long componentId : baseComponentIds) {
            if (!activeComponentsById.containsKey(componentId)) {
                validateBaseComponent(domain, componentService.getById(componentId));
                throw new ValidationException(
                        "Component with id {} is unavailable for configurator search",
                        componentId
                );
            }
        }
        CompatibilityGraphContext context = buildCompatibilityContext(
                domainId,
                activeComponents
        );

        List<ConfiguratorResult> results = new ArrayList<>(baseComponentIds.size());
        for (Long baseComponentId : baseComponentIds) {
            Component baseComponent = activeComponentsById.get(baseComponentId);
            List<Component> candidates = activeComponents.stream()
                    .filter(component -> !component.getId().equals(baseComponentId))
                    .toList();
            Map<Long, List<CompatibleComponent>> compatibleByType = findCompatibilityInGraph(
                    baseComponent,
                    candidates,
                    context,
                    includeTransitive
            );
            results.add(ConfiguratorResult.builder()
                    .baseComponentId(baseComponentId)
                    .compatibleByType(toOrderedGroups(domain, compatibleByType))
                    .build());
        }
        return ConfiguratorBatchResult.builder()
                .results(List.copyOf(results))
                .build();
    }

    private Map<Long, List<CompatibleComponent>> findDirectCompatibility(
            Long domainId,
            Component baseComponent,
            List<Component> candidates
    ) {
        List<CompatibilityLink> manualLinks =
                configuratorRepository.getManualCompatibilityLinks(
                        domainId,
                        baseComponent.getId()
                );
        List<CompatibilityRuleSet> automaticRules =
                compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(
                        domainId,
                        baseComponent.getComponentTypeId()
                );

        Map<Long, List<CompatibilityRuleSet>> rulesByCandidateType =
                indexRulesByCandidateType(automaticRules, baseComponent.getComponentTypeId());
        Map<Long, List<CompatibilityExplanation>> manualExplanations =
                indexManualExplanations(manualLinks, baseComponent.getId());
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
        return compatibleByType;
    }

    private Map<Long, List<CompatibleComponent>> findTransitiveCompatibility(
            Long domainId,
            Component baseComponent,
            List<Component> candidates
    ) {
        List<Component> activeComponents = new ArrayList<>(candidates.size() + 1);
        activeComponents.add(baseComponent);
        activeComponents.addAll(candidates);

        return findCompatibilityInGraph(
                baseComponent,
                candidates,
                buildCompatibilityContext(domainId, activeComponents),
                true
        );
    }

    private CompatibilityGraphContext buildCompatibilityContext(
            Long domainId,
            List<Component> activeComponents
    ) {
        Map<Long, Component> componentsById = new HashMap<>();
        Map<Long, List<Component>> componentsByType = new HashMap<>();
        Map<Long, Integer> componentOrder = new HashMap<>();
        for (int index = 0; index < activeComponents.size(); index++) {
            Component component = activeComponents.get(index);
            componentsById.put(component.getId(), component);
            componentsByType.computeIfAbsent(
                    component.getComponentTypeId(),
                    ignored -> new ArrayList<>()
            ).add(component);
            componentOrder.put(component.getId(), index);
        }

        Map<Long, Map<Long, List<CompatibilityExplanation>>> graph = new HashMap<>();
        addManualEdges(
                graph,
                configuratorRepository.getAllManualCompatibilityLinks(domainId),
                componentsById.keySet()
        );
        addAutomaticEdges(
                graph,
                compatibilityRuleRepository.getEnabledByDomainId(domainId),
                componentsByType
        );

        return new CompatibilityGraphContext(graph, componentOrder);
    }

    private static Map<Long, List<CompatibleComponent>> findCompatibilityInGraph(
            Component baseComponent,
            List<Component> candidates,
            CompatibilityGraphContext context,
            boolean includeTransitive
    ) {
        Map<Long, Long> predecessors = includeTransitive
                ? findShortestPaths(
                        context.graph(),
                        baseComponent.getId(),
                        context.componentOrder()
                )
                : Map.of();
        Map<Long, List<CompatibleComponent>> compatibleByType = new HashMap<>();
        Map<Long, List<CompatibilityExplanation>> baseComponentNeighbours = context.graph()
                .getOrDefault(baseComponent.getId(), Map.of());
        for (Component candidate : candidates) {
            List<CompatibilityExplanation> directExplanations = baseComponentNeighbours
                    .getOrDefault(candidate.getId(), List.of());
            if (!directExplanations.isEmpty()) {
                addCompatibleComponent(compatibleByType, candidate, directExplanations);
            } else if (predecessors.containsKey(candidate.getId())) {
                CompatibilityExplanation explanation = CompatibilityExplanation.builder()
                        .source(CompatibilityExplanationSource.TRANSITIVE)
                        .pathComponentIds(buildPath(
                                predecessors,
                                baseComponent.getId(),
                                candidate.getId()
                        ))
                        .build();
                addCompatibleComponent(compatibleByType, candidate, List.of(explanation));
            }
        }
        return compatibleByType;
    }

    private static void validateBatchComponentIds(List<Long> componentIds) {
        if (componentIds == null || componentIds.isEmpty()) {
            throw new ValidationException("At least one component id is required");
        }
        if (componentIds.size() > MAX_BATCH_COMPONENTS) {
            throw new ValidationException(
                    "No more than {} component ids are allowed",
                    MAX_BATCH_COMPONENTS
            );
        }
        if (componentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ValidationException("Component identifiers must be positive");
        }
        if (new HashSet<>(componentIds).size() != componentIds.size()) {
            throw new ValidationException("Component identifiers must be unique");
        }
    }

    private static void addManualEdges(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            List<CompatibilityLink> links,
            Set<Long> activeComponentIds
    ) {
        for (CompatibilityLink link : links) {
            if (!activeComponentIds.contains(link.componentAId())
                    || !activeComponentIds.contains(link.componentBId())) {
                continue;
            }
            CompatibilityExplanation explanation = CompatibilityExplanation.builder()
                    .source(CompatibilityExplanationSource.MANUAL)
                    .linkId(link.id())
                    .comment(link.comment())
                    .build();
            addUndirectedEdge(
                    graph,
                    link.componentAId(),
                    link.componentBId(),
                    explanation
            );
        }
    }

    private void addAutomaticEdges(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            List<CompatibilityRuleSet> rules,
            Map<Long, List<Component>> componentsByType
    ) {
        for (CompatibilityRuleSet rule : rules) {
            List<Component> componentsA =
                    componentsByType.getOrDefault(rule.componentTypeAId(), List.of());
            List<Component> componentsB =
                    componentsByType.getOrDefault(rule.componentTypeBId(), List.of());
            for (Component componentA : componentsA) {
                for (Component componentB : componentsB) {
                    compatibilityRuleEvaluator.evaluate(rule, componentA, componentB)
                            .map(ConfiguratorServiceImpl::toAutomaticExplanation)
                            .ifPresent(explanation -> addUndirectedEdge(
                                    graph,
                                    componentA.getId(),
                                    componentB.getId(),
                                    explanation
                            ));
                }
            }
        }
    }

    private static void addUndirectedEdge(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            Long componentAId,
            Long componentBId,
            CompatibilityExplanation explanation
    ) {
        addDirectedEdge(graph, componentAId, componentBId, explanation);
        addDirectedEdge(graph, componentBId, componentAId, explanation);
    }

    private static void addDirectedEdge(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            Long sourceId,
            Long targetId,
            CompatibilityExplanation explanation
    ) {
        graph.computeIfAbsent(sourceId, ignored -> new HashMap<>())
                .computeIfAbsent(targetId, ignored -> new ArrayList<>())
                .add(explanation);
    }

    private static Map<Long, Long> findShortestPaths(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            Long baseComponentId,
            Map<Long, Integer> componentOrder
    ) {
        Map<Long, Long> predecessors = new HashMap<>();
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        visited.add(baseComponentId);
        queue.add(baseComponentId);

        Comparator<Long> byComponentOrder = Comparator.comparingInt(
                id -> componentOrder.getOrDefault(id, Integer.MAX_VALUE)
        );
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            List<Long> neighbours = graph.getOrDefault(current, Map.of())
                    .keySet()
                    .stream()
                    .sorted(byComponentOrder)
                    .toList();
            for (Long neighbour : neighbours) {
                if (visited.add(neighbour)) {
                    predecessors.put(neighbour, current);
                    queue.addLast(neighbour);
                }
            }
        }
        return predecessors;
    }

    private static List<Long> buildPath(
            Map<Long, Long> predecessors,
            Long baseComponentId,
            Long targetComponentId
    ) {
        List<Long> reversedPath = new ArrayList<>();
        Long current = targetComponentId;
        while (current != null) {
            reversedPath.add(current);
            if (current.equals(baseComponentId)) {
                break;
            }
            current = predecessors.get(current);
        }
        Collections.reverse(reversedPath);
        return List.copyOf(reversedPath);
    }

    private static void addCompatibleComponent(
            Map<Long, List<CompatibleComponent>> compatibleByType,
            Component component,
            List<CompatibilityExplanation> explanations
    ) {
        compatibleByType.computeIfAbsent(
                component.getComponentTypeId(),
                ignored -> new ArrayList<>()
        ).add(toCompatibleComponent(component, explanations));
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

    private record CompatibilityGraphContext(
            Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
            Map<Long, Integer> componentOrder
    ) {
    }
}
