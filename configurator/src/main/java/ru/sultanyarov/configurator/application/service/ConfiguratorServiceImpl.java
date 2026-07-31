package ru.sultanyarov.configurator.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRuleRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ConfiguratorBatchResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorIntersectionResult;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;
import ru.sultanyarov.configurator.domain.model.Domain;

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
      Long domainId, Long baseComponentId, boolean includeTransitive) {
    log.debug(
        "get compatible components in domain {} for base component {}, include transitive: {}",
        domainId,
        baseComponentId,
        includeTransitive);
    Domain domain = domainService.getById(domainId);
    Component baseComponent = componentService.getById(baseComponentId);
    validateBaseComponent(domain, baseComponent);

    List<Component> candidates =
        configuratorRepository.getActiveCandidates(domainId, baseComponentId);
    Map<Long, List<CompatibleComponent>> compatibleByType =
        includeTransitive
            ? findTransitiveCompatibility(domainId, baseComponent, candidates)
            : findDirectCompatibility(domainId, baseComponent, candidates);

    return ConfiguratorResult.builder()
        .baseComponentId(baseComponentId)
        .compatibleByType(ConfiguratorResultAssembler.toOrderedGroups(domain, compatibleByType))
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public ConfiguratorBatchResult searchCompatibleComponents(
      Long domainId, List<Long> baseComponentIds, boolean includeTransitive) {
    log.debug(
        "search compatible components in domain {} for base components {}, "
            + "include transitive: {}",
        domainId,
        baseComponentIds,
        includeTransitive);
    validateBatchComponentIds(baseComponentIds);
    SearchContext searchContext = prepareSearchContext(domainId, baseComponentIds);
    CompatibilityGraphContext graphContext =
        buildGraphContext(domainId, searchContext.activeComponents());

    List<ConfiguratorResult> results =
        baseComponentIds.stream()
            .map(
                baseComponentId ->
                    searchForBase(baseComponentId, searchContext, graphContext, includeTransitive))
            .toList();
    return ConfiguratorBatchResult.builder().results(List.copyOf(results)).build();
  }

  @Override
  @Transactional(readOnly = true)
  public ConfiguratorIntersectionResult intersectCompatibleComponents(
      Long domainId, List<Long> baseComponentIds, boolean includeTransitive) {
    log.debug(
        "intersect compatible components in domain {} for base components {}, "
            + "include transitive: {}",
        domainId,
        baseComponentIds,
        includeTransitive);
    validateIntersectionComponentIds(baseComponentIds);
    SearchContext searchContext = prepareSearchContext(domainId, baseComponentIds);
    List<Component> candidates =
        excludeSelectedComponents(searchContext.activeComponents(), baseComponentIds);
    CompatibilityGraphContext graphContext =
        buildGraphContext(domainId, searchContext.activeComponents());
    var compatibilityByCandidate =
        CompatibilityGraphSearchEngine.intersectByCandidate(
            baseComponentIds,
            searchContext.activeComponentsById(),
            candidates,
            graphContext,
            includeTransitive);

    return ConfiguratorIntersectionResult.builder()
        .componentIds(List.copyOf(baseComponentIds))
        .compatibleByType(
            ConfiguratorResultAssembler.toOrderedIntersectionGroups(
                searchContext.domain(),
                candidates,
                compatibilityByCandidate,
                baseComponentIds.size()))
        .build();
  }

  private Map<Long, List<CompatibleComponent>> findDirectCompatibility(
      Long domainId, Component baseComponent, List<Component> candidates) {
    return DirectCompatibilityResolver.resolve(
        baseComponent,
        candidates,
        configuratorRepository.getManualCompatibilityLinks(domainId, baseComponent.getId()),
        compatibilityRuleRepository.getEnabledByDomainIdAndComponentTypeId(
            domainId, baseComponent.getComponentTypeId()),
        compatibilityRuleEvaluator);
  }

  private Map<Long, List<CompatibleComponent>> findTransitiveCompatibility(
      Long domainId, Component baseComponent, List<Component> candidates) {
    List<Component> activeComponents = new ArrayList<>(candidates.size() + 1);
    activeComponents.add(baseComponent);
    activeComponents.addAll(candidates);
    return CompatibilityGraphSearchEngine.findCompatibility(
        baseComponent, candidates, buildGraphContext(domainId, activeComponents), true);
  }

  private ConfiguratorResult searchForBase(
      Long baseComponentId,
      SearchContext searchContext,
      CompatibilityGraphContext graphContext,
      boolean includeTransitive) {
    Component baseComponent = searchContext.activeComponentsById().get(baseComponentId);
    List<Component> candidates =
        searchContext.activeComponents().stream()
            .filter(component -> !component.getId().equals(baseComponentId))
            .toList();
    Map<Long, List<CompatibleComponent>> compatibleByType =
        CompatibilityGraphSearchEngine.findCompatibility(
            baseComponent, candidates, graphContext, includeTransitive);
    return ConfiguratorResult.builder()
        .baseComponentId(baseComponentId)
        .compatibleByType(
            ConfiguratorResultAssembler.toOrderedGroups(searchContext.domain(), compatibleByType))
        .build();
  }

  private SearchContext prepareSearchContext(Long domainId, List<Long> baseComponentIds) {
    Domain domain = domainService.getById(domainId);
    List<Component> activeComponents = configuratorRepository.getActiveComponents(domainId);
    Map<Long, Component> activeComponentsById = indexComponentsById(activeComponents);
    validateBatchBaseComponents(domain, baseComponentIds, activeComponentsById);
    return new SearchContext(domain, activeComponents, activeComponentsById);
  }

  private CompatibilityGraphContext buildGraphContext(
      Long domainId, List<Component> activeComponents) {
    return CompatibilityGraphBuilder.build(
        activeComponents,
        configuratorRepository.getAllManualCompatibilityLinks(domainId),
        compatibilityRuleRepository.getEnabledByDomainId(domainId),
        compatibilityRuleEvaluator);
  }

  private static Map<Long, Component> indexComponentsById(List<Component> components) {
    Map<Long, Component> result = new HashMap<>();
    for (Component component : components) {
      result.put(component.getId(), component);
    }
    return result;
  }

  private static List<Component> excludeSelectedComponents(
      List<Component> activeComponents, List<Long> selectedComponentIds) {
    Set<Long> selectedIds = new HashSet<>(selectedComponentIds);
    return activeComponents.stream()
        .filter(component -> !selectedIds.contains(component.getId()))
        .toList();
  }

  private static void validateBatchComponentIds(List<Long> componentIds) {
    if (componentIds == null || componentIds.isEmpty()) {
      throw new ValidationException("At least one component id is required");
    }
    if (componentIds.size() > MAX_BATCH_COMPONENTS) {
      throw new ValidationException(
          "No more than {} component ids are allowed", MAX_BATCH_COMPONENTS);
    }
    if (componentIds.stream().anyMatch(id -> id == null || id <= 0)) {
      throw new ValidationException("Component identifiers must be positive");
    }
    if (new HashSet<>(componentIds).size() != componentIds.size()) {
      throw new ValidationException("Component identifiers must be unique");
    }
  }

  private static void validateIntersectionComponentIds(List<Long> componentIds) {
    if (componentIds == null || componentIds.size() < 2) {
      throw new ValidationException("At least two component ids are required");
    }
    validateBatchComponentIds(componentIds);
  }

  private void validateBatchBaseComponents(
      Domain domain, List<Long> componentIds, Map<Long, Component> activeComponentsById) {
    for (Long componentId : componentIds) {
      if (activeComponentsById.containsKey(componentId)) {
        continue;
      }
      validateBaseComponent(domain, componentService.getById(componentId));
      throw new ValidationException(
          "Component with id {} is unavailable for configurator search", componentId);
    }
  }

  private static void validateBaseComponent(Domain domain, Component baseComponent) {
    boolean belongsToDomain =
        domain.componentTypes() != null
            && domain.componentTypes().stream()
                .anyMatch(type -> type.id().equals(baseComponent.getComponentTypeId()));
    if (!belongsToDomain) {
      throw new ValidationException(
          "Component with id {} does not belong to domain with id {}",
          baseComponent.getId(),
          domain.id());
    }
    if (Boolean.TRUE.equals(baseComponent.getArchived())) {
      throw new ValidationException(
          "Archived component with id {} cannot be used as configurator base",
          baseComponent.getId());
    }
  }

  private record SearchContext(
      Domain domain, List<Component> activeComponents, Map<Long, Component> activeComponentsById) {}
}
