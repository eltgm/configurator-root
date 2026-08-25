package ru.sultanyarov.configurator.application.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyPairDecision;
import ru.sultanyarov.configurator.domain.model.ConfiguratorAssemblyStatus;
import ru.sultanyarov.configurator.domain.model.PairCompatibilityStatus;

@UtilityClass
class AssemblyCompatibilityEvaluator {
  static AssemblyCompatibilityEvaluation evaluate(
      List<Component> components, CompatibilityDecisionResolver decisionResolver) {
    Map<Long, Set<Long>> allowedGraph = new HashMap<>();
    components.forEach(component -> allowedGraph.put(component.getId(), new HashSet<>()));
    List<ConfiguratorAssemblyPairDecision> pairDecisions = new ArrayList<>();
    boolean blocked = false;

    for (int leftIndex = 0; leftIndex < components.size(); leftIndex++) {
      for (int rightIndex = leftIndex + 1; rightIndex < components.size(); rightIndex++) {
        Component left = components.get(leftIndex);
        Component right = components.get(rightIndex);
        var decision = decisionResolver.resolve(left, right);
        pairDecisions.add(
            ConfiguratorAssemblyPairDecision.builder()
                .leftComponentId(left.getId())
                .rightComponentId(right.getId())
                .status(decision.status())
                .explanations(decision.explanations())
                .blockingRules(decision.blockingRules())
                .build());
        if (decision.status() == PairCompatibilityStatus.DENIED) {
          blocked = true;
        } else if (decision.status() == PairCompatibilityStatus.ALLOWED) {
          allowedGraph.get(left.getId()).add(right.getId());
          allowedGraph.get(right.getId()).add(left.getId());
        }
      }
    }

    if (blocked) {
      return new AssemblyCompatibilityEvaluation(
          ConfiguratorAssemblyStatus.BLOCKED, List.copyOf(pairDecisions), null);
    }
    Long disconnectedComponentId = firstDisconnectedComponent(components, allowedGraph);
    return new AssemblyCompatibilityEvaluation(
        disconnectedComponentId == null
            ? ConfiguratorAssemblyStatus.VALID
            : ConfiguratorAssemblyStatus.DISCONNECTED,
        List.copyOf(pairDecisions),
        disconnectedComponentId);
  }

  private static Long firstDisconnectedComponent(
      List<Component> components, Map<Long, Set<Long>> allowedGraph) {
    if (components.size() < 2) {
      return null;
    }
    Long rootId = components.getFirst().getId();
    Set<Long> visited = new HashSet<>();
    ArrayDeque<Long> queue = new ArrayDeque<>();
    visited.add(rootId);
    queue.add(rootId);
    while (!queue.isEmpty()) {
      Long currentId = queue.removeFirst();
      for (Long adjacentId : allowedGraph.getOrDefault(currentId, Set.of())) {
        if (visited.add(adjacentId)) {
          queue.addLast(adjacentId);
        }
      }
    }
    return components.stream()
        .map(Component::getId)
        .filter(componentId -> !visited.contains(componentId))
        .findFirst()
        .orElse(null);
  }
}

record AssemblyCompatibilityEvaluation(
    ConfiguratorAssemblyStatus status,
    List<ConfiguratorAssemblyPairDecision> pairDecisions,
    Long disconnectedComponentId) {}
