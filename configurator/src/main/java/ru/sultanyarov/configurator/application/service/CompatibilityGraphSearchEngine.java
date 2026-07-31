package ru.sultanyarov.configurator.application.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.BaseComponentCompatibility;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.Component;

@UtilityClass
class CompatibilityGraphSearchEngine {
  static Map<Long, List<CompatibleComponent>> findCompatibility(
      Component baseComponent,
      List<Component> candidates,
      CompatibilityGraphContext context,
      boolean includeTransitive) {
    Map<Long, Long> predecessors =
        includeTransitive
            ? findShortestPaths(context.graph(), baseComponent.getId(), context.componentOrder())
            : Map.of();
    Map<Long, List<CompatibleComponent>> compatibleByType = new HashMap<>();
    Map<Long, List<CompatibilityExplanation>> directNeighbours =
        context.graph().getOrDefault(baseComponent.getId(), Map.of());

    for (Component candidate : candidates) {
      addCandidateWhenCompatible(
          baseComponent, candidate, directNeighbours, predecessors, compatibleByType);
    }
    return compatibleByType;
  }

  private static void addCandidateWhenCompatible(
      Component baseComponent,
      Component candidate,
      Map<Long, List<CompatibilityExplanation>> directNeighbours,
      Map<Long, Long> predecessors,
      Map<Long, List<CompatibleComponent>> compatibleByType) {
    List<CompatibilityExplanation> directExplanations =
        directNeighbours.getOrDefault(candidate.getId(), List.of());
    if (!directExplanations.isEmpty()) {
      addCompatibleComponent(compatibleByType, candidate, directExplanations);
      return;
    }
    if (predecessors.containsKey(candidate.getId())) {
      CompatibilityExplanation transitiveExplanation =
          CompatibilityExplanation.builder()
              .source(CompatibilityExplanationSource.TRANSITIVE)
              .pathComponentIds(buildPath(predecessors, baseComponent.getId(), candidate.getId()))
              .build();
      addCompatibleComponent(compatibleByType, candidate, List.of(transitiveExplanation));
    }
  }

  /**
   * Intersects compatibility sets incrementally while preserving the order of base components.
   *
   * <p>The first base component creates the initial candidate set. Every subsequent base walks only
   * that reduced set: an incompatible candidate is removed, while a compatible one receives another
   * {@link BaseComponentCompatibility} entry. Therefore a candidate remaining at the end is
   * compatible with every selected base, and its evidence list follows {@code baseComponentIds}
   * order.
   */
  static Map<Long, List<BaseComponentCompatibility>> intersectByCandidate(
      List<Long> baseComponentIds,
      Map<Long, Component> activeComponentsById,
      List<Component> candidates,
      CompatibilityGraphContext context,
      boolean includeTransitive) {
    Map<Long, List<BaseComponentCompatibility>> intersection = new HashMap<>();
    boolean initializeIntersection = true;

    for (Long baseComponentId : baseComponentIds) {
      Map<Long, CompatibleComponent> compatibleComponents =
          findCompatibleComponentsById(
              activeComponentsById.get(baseComponentId), candidates, context, includeTransitive);
      if (initializeIntersection) {
        initializeIntersection(intersection, candidates, compatibleComponents, baseComponentId);
        initializeIntersection = false;
      } else {
        retainCompatibleCandidates(intersection, compatibleComponents, baseComponentId);
      }
    }
    return intersection;
  }

  private static Map<Long, CompatibleComponent> findCompatibleComponentsById(
      Component baseComponent,
      List<Component> candidates,
      CompatibilityGraphContext context,
      boolean includeTransitive) {
    Map<Long, List<CompatibleComponent>> compatibleByType =
        findCompatibility(baseComponent, candidates, context, includeTransitive);
    Map<Long, CompatibleComponent> result = new HashMap<>();
    for (List<CompatibleComponent> compatibleComponents : compatibleByType.values()) {
      for (CompatibleComponent compatibleComponent : compatibleComponents) {
        result.put(compatibleComponent.id(), compatibleComponent);
      }
    }
    return result;
  }

  private static void initializeIntersection(
      Map<Long, List<BaseComponentCompatibility>> intersection,
      List<Component> candidates,
      Map<Long, CompatibleComponent> compatibleComponents,
      Long baseComponentId) {
    for (Component candidate : candidates) {
      CompatibleComponent compatibleComponent = compatibleComponents.get(candidate.getId());
      if (compatibleComponent != null) {
        intersection.put(
            candidate.getId(),
            new ArrayList<>(List.of(toBaseCompatibility(baseComponentId, compatibleComponent))));
      }
    }
  }

  private static void retainCompatibleCandidates(
      Map<Long, List<BaseComponentCompatibility>> intersection,
      Map<Long, CompatibleComponent> compatibleComponents,
      Long baseComponentId) {
    Iterator<Map.Entry<Long, List<BaseComponentCompatibility>>> iterator =
        intersection.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<Long, List<BaseComponentCompatibility>> entry = iterator.next();
      CompatibleComponent compatibleComponent = compatibleComponents.get(entry.getKey());
      if (compatibleComponent == null) {
        iterator.remove();
      } else {
        entry.getValue().add(toBaseCompatibility(baseComponentId, compatibleComponent));
      }
    }
  }

  /**
   * Runs breadth-first search (BFS) from the selected component.
   *
   * <p>The FIFO queue processes all vertices at distance N before vertices at distance N + 1.
   * Consequently, the first time a neighbour is visited, the stored predecessor belongs to a
   * shortest path in the unweighted compatibility graph. Sorting neighbours by component order
   * makes the selected shortest path deterministic when several paths have the same length. The
   * visited set also prevents cycles from putting the same vertex into the queue twice.
   */
  private static Map<Long, Long> findShortestPaths(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      Long baseComponentId,
      Map<Long, Integer> componentOrder) {
    Map<Long, Long> predecessors = new HashMap<>();
    Set<Long> visited = new HashSet<>();
    ArrayDeque<Long> queue = new ArrayDeque<>();
    visited.add(baseComponentId);
    queue.add(baseComponentId);

    Comparator<Long> byComponentOrder =
        Comparator.comparingInt(id -> componentOrder.getOrDefault(id, Integer.MAX_VALUE));
    while (!queue.isEmpty()) {
      Long current = queue.removeFirst();
      List<Long> neighbours =
          graph.getOrDefault(current, Map.of()).keySet().stream().sorted(byComponentOrder).toList();
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
      Map<Long, Long> predecessors, Long baseComponentId, Long targetComponentId) {
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
      List<CompatibilityExplanation> explanations) {
    compatibleByType
        .computeIfAbsent(component.getComponentTypeId(), ignored -> new ArrayList<>())
        .add(
            CompatibleComponent.builder()
                .id(component.getId())
                .name(component.getName())
                .brand(component.getBrand())
                .componentTypeId(component.getComponentTypeId())
                .explanations(List.copyOf(explanations))
                .build());
  }

  private static BaseComponentCompatibility toBaseCompatibility(
      Long baseComponentId, CompatibleComponent compatibleComponent) {
    return BaseComponentCompatibility.builder()
        .baseComponentId(baseComponentId)
        .explanations(compatibleComponent.explanations())
        .build();
  }
}
